package analysis;

/**
 * 
 * @author alex-
 * @version 1
 * 
 * Klasse um Items aus Weka zu entnehmen
 */

public class Item implements Comparable<Item>{

	String name;
	int anzahl;
	
	/**
	 * Modifizierung der Attribute aus Weka
	 * @param name
	 * @param n
	 */
	
	public Item(String name, int n) {
		
		//Extract Name form Weka attribute String
		name = name.replace("@attribute ", "");
		name = name.replace("\'", "");
		name = name.replace(" {1}", "");
		
		this.name = name;
		this.anzahl = n;
	}
	
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Item o) {
		if(anzahl > o.anzahl) {
			return -1;
		}else if(anzahl < o.anzahl) {
			return 1;
		}
		return 0;
	}
	
}

