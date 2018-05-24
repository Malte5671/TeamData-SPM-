package analysis;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import weka.associations.Apriori;
import weka.associations.AssociationRule;
import weka.associations.AssociationRules;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericCleaner;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * @author Kai
 *
 */
public class Analysis implements Serializable{

	private static final long serialVersionUID = 1L;
	List<String> topItems;
	Map<String, Integer> customersWeekday;
	Map<String, Integer> customersDaytime;
	String name;
	List<String> itemSets;
	
	public Analysis() {
		customersDaytime = new HashMap<>();
		customersWeekday = new HashMap<>();
		topItems = new ArrayList<>();
		name = "Default";
	}
	
	public Analysis(InputStream csv, String name) throws Exception {
		
		this.name = name;
		Instances data = null;
		List<Item> items;
		topItems = new ArrayList<>();
		
		//load CSV-File into Weka
		CSVLoader loader = new CSVLoader();		
		try {
			loader.setSource(csv);
			data = loader.getDataSet();	
			data = numericToNominal(data);
		} catch (Exception e) {
			throw e;
		}
		
		if(data.isEmpty()) {
			throw new Exception();
		}
		
		//get Top 5 items from data
		items = getTop5Items(data);
		for(Item i: items) {
			topItems.add(i.toString());
		}
		
		//get customers per weekday
		customersWeekday = customersPerWeekday(data);
		
		//get customers per daytime
		customersDaytime = customersPerDaytime(data);
		
		//H�ufig zusammmengekaufte Waren
		itemSets = searchForItemSets(data);
		
		
	}
	
	private Instances numericToNominal(Instances data) throws Exception {
		
		// 0 durch ? ersetzen, um f�r die Auswertung nur die Waren zu
		// ber�cksichtigen, die gekauft wurden
		NumericCleaner nc = new NumericCleaner();
		nc.setMinThreshold(1.0); // Schwellwert auf 1 setzen
		nc.setMinDefault(Double.NaN); // alles unter Schwellwert durch ? ersetzen
		nc.setInputFormat(data);
		data = Filter.useFilter(data, nc); // Filter anwenden.
		
		// Die Daten als nominale und nicht als numerische Daten setzen
		NumericToNominal num2nom = new NumericToNominal();
		num2nom.setAttributeIndices("first-last");
		num2nom.setInputFormat(data);
		return Filter.useFilter(data, num2nom);
		
	}
	
	private List<Item> getTop5Items(Instances data){
		
		ArrayList<Item> items = new ArrayList<>();
		
		
		//Get items out of Data
		for(int i = 7; i < 24; i++) {
			items.add(new Item(data.attribute(i).name(), data.attributeStats(i).totalCount - data.attributeStats(i).missingCount));
		}
		
		Collections.sort(items);
		
		//Shrink list to top 5 items
		for(int i=items.size()-1; i>4; i--) {
			items.remove(i);
		}

		return items;
		
	}
	
	private Map<String, Integer> customersPerWeekday(Instances data) throws Exception {
		//extract weekday attribute from data
		Instances einkaufsTage = new Instances(data);
		Remove remove = new Remove();
		remove.setAttributeIndices("6");
		remove.setInvertSelection(true);
		remove.setInputFormat(einkaufsTage);
		einkaufsTage = Filter.useFilter(einkaufsTage, remove);
		
		Enumeration<Object> e = einkaufsTage.attribute(0).enumerateValues();
		Map<String, Integer> mapCustomerToDay = new HashMap<>();
		
		//Add Values to Map
		for(int i=0; i<einkaufsTage.attribute(0).numValues(); i++) {
			mapCustomerToDay.put(e.nextElement().toString(), einkaufsTage.attributeStats(0).nominalCounts[i]);
		}
		
		return mapCustomerToDay;	
	}
	
	private Map<String, Integer> customersPerDaytime(Instances data) throws Exception{
		
		//extract daytime attribute from data
		Instances einkaufsZeiten = new Instances(data);
		Remove remove = new Remove();
		remove.setAttributeIndices("7");
		remove.setInvertSelection(true);
		remove.setInputFormat(einkaufsZeiten);
		einkaufsZeiten = Filter.useFilter(einkaufsZeiten, remove);
		
		Enumeration<Object> e = einkaufsZeiten.attribute(0).enumerateValues();
		Map<String, Integer> mapCustomerToDaytime = new HashMap<>();
		
		//Add Values to Map
		for(int i=0; i<einkaufsZeiten.attribute(0).numValues(); i++) {
			mapCustomerToDaytime.put(e.nextElement().toString(), einkaufsZeiten.attributeStats(0).nominalCounts[i]);
		}
		
		return mapCustomerToDaytime;
		
	}
	
	private List<String> searchForItemSets(Instances data){
		
		Set<String> itemSetsSet = new HashSet<>();
		List<String> itemSets = new ArrayList<>();
		
		// Apriori anwenden

			// Kundendaten rausnehmen, nur Warenk�rbe stehen lassen
			Instances onlyItems = new Instances(data);
			for (int i = 0; i < 7; i++) {
				onlyItems.deleteAttributeAt(0); // ein einzelnes Attribut rausnehmen
			}
			
			Apriori warenModel = new Apriori();
			
			
			//warenModel.setNumRules(10); // die besten drei Ergebnisse
	
			try {
				warenModel.buildAssociations(onlyItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			AssociationRules list = warenModel.getAssociationRules();
			
			//Alle generierten Regeln durchgehen
			for(AssociationRule rule: list.getRules()) {
				ArrayList<String> set = new ArrayList<>();
				String s = rule.getPremise().toString();
				s = s.replaceAll("=1", "");
				s = s.replace("[", "");
				s = s.replace("]", "");
				s = s.replaceAll(" ", "");
				String[] split = s.split(",");
				set.addAll(Arrays.asList(split));
			
				s = rule.getConsequence().toString();
				s = s.replaceAll("=1", "");
				s = s.replace("[", "");
				s = s.replace("]", "");
				s = s.replaceAll(" ", "");
				split = s.split(",");
				set.addAll(Arrays.asList(split));
				
				Collections.sort(set);
				
				s = "";
				for(String item: set) {
					int index = set.indexOf(item);
					if(index==set.size()-1) {
						s += " und " + item;
					}else if(index==0){
						s += item;
					}else{
						s += ", " + item;
					}
					
				}
				
				itemSetsSet.add(s);
			}
			
			for(String s: itemSetsSet) {
				itemSets.add(s);
			}
			
			return itemSets;
	}

	public List<String> getTopItems() {
		return topItems;
	}

	public void setTopItems(List<String> topItems) {
		this.topItems = topItems;
	}

	public Map<String, Integer> getCustomersWeekday() {
		return customersWeekday;
	}

	public void setCustomersWeekday(Map<String, Integer> customersWeekday) {
		this.customersWeekday = customersWeekday;
	}

	public Map<String, Integer> getCustomersDaytime() {
		return customersDaytime;
	}

	public void setCustomersDaytime(Map<String, Integer> customersDaytime) {
		this.customersDaytime = customersDaytime;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getItemSets() {
		return itemSets;
	}

	public void setItemSets(List<String> itemSets) {
		this.itemSets = itemSets;
	}
	
}
