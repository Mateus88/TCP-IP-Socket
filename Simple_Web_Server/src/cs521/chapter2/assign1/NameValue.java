package cs521.chapter2.assign1;

public class NameValue 
{
	private String name;
	private String value;
	
	public NameValue() {
		this.name = null;
		this.value = null;
	}
	
	public NameValue(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
