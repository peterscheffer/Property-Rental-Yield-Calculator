import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import java.text.ParseException;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import java.lang.StringBuffer;
import java.lang.Number;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RentalYieldProvider {

	private static NumberFormat nf = NumberFormat.getCurrencyInstance();
	private static final int MAX_BEDROOMS = 5;
	private static int counter = 1;
	private static Hashtable<Integer, Double> meanHousePurchaseValues = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> meanUnitPurchaseValues = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> meanHouseRentValues = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> meanUnitRentValues = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> maxHousePurchaseValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> minHousePurchaseValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> maxUnitPurchaseValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> minUnitPurchaseValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> maxHouseRentValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> minHouseRentValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> maxUnitRentValue = new Hashtable<Integer, Double>();
	private static Hashtable<Integer, Double> minUnitRentValue = new Hashtable<Integer, Double>();
	private static int propertyListingCount = 0;
	private static final int PROPERTIES_PER_PAGE = 10;

	public static void main(String[] args) {
		String[] towns = args;
		String state = "VIC";

		if (args.length == 2) {
			state = args[1];
		} else if (args.length == 0) {
			towns = new String[suburbList.length];
			System.arraycopy(suburbList, 0, towns, 0, suburbList.length);
		}

		createSummaryTable();

		for (String town : towns) {
			meanHousePurchaseValues = new Hashtable<Integer, Double>();
			meanUnitPurchaseValues = new Hashtable<Integer, Double>();
			maxHousePurchaseValue = new Hashtable<Integer, Double>();
			minHousePurchaseValue = new Hashtable<Integer, Double>();
			maxUnitPurchaseValue = new Hashtable<Integer, Double>();
			minUnitPurchaseValue = new Hashtable<Integer, Double>();

			createTable(town);

			List<Property> propertyValues = getPropertyValues("Buy", town,
					state);
			writeToSpreadsheet(propertyValues, "Buy", town);

			propertyValues = getPropertyValues("Rent", town, state);
			writeToSpreadsheet(propertyValues, "Rent", town);
		}
	}

	private static List<Property> getPropertyValues(String type, String town,
			String state) {
		int index1 = 0, index2 = 0, index3 = 0, index4 = 0, index5 = 0;
		int index6 = 0, index7 = 0, index8 = 0, index9 = 0, index10 = 0, index11 = 0, index12 = 0, index13 = 0;
		String lt = "<";
		String gt = ">";
		String string1 = "<dl class=\"cB-featDetails\">";
		String string2 = "</dl>";
		String string3 = "class=\"price\"";
		String string4 = "</dd>";
		String string5 = "<dd class=\"bedrooms\"";
		String string6 = "<dd class=\"bathrooms\"";
		String string7 = "<dd class=\"propertytype\">";
		String string8 = "cT-searchHeading";
		String string9 = "<em>";

		String details = "";
		String price = "";
		String bedrooms = "";
		String bathrooms = "";
		String propertyType = "";
		String propertyCount = "";
		Number amount = null;
		StringBuffer buffer = null;
		NumberFormat df = new DecimalFormat("#");
		ArrayList<Property> properties = new ArrayList<Property>();
		long numberOfPages = 1;

		String buyWebpageURL = "http://www.domain.com.au/Public/SearchResults.aspx?mode=buy&state="
				+ state
				+ "&sub="
				+ URLEncoder.encode(town)
				+ "&proptypes=1,2,4&ptdes=House%2c+Unit%2fFlat%2fApartment%2c+Townhouse&bedrooms=2,3,4,%3E5&extype=3&page=";
		String rentWebpageURL = "http://www.domain.com.au/Public/SearchResults.aspx?mode=rent&Refine=1&state="
				+ state
				+ "&sub="
				+ URLEncoder.encode(town)
				+ "&ssubs=0&bedrooms=%3E2&from=0&to=2147483647&proptypes=U,H,N&ptdes=House%2c+Unit%2fFlat%2fApartment%2c+Townhouse&page=";

		if (type.equals("Buy")) {
			buffer = new StringBuffer(getWebpage(buyWebpageURL));
		} else {
			buffer = new StringBuffer(getWebpage(rentWebpageURL));
		}

		index12 = buffer.indexOf(string8);
		index13 = buffer.indexOf(string9, index12) + string9.length();

		propertyCount = buffer.substring(index13, buffer.indexOf(lt, index13));

		if (propertyCount.equals("")) {
			return properties;
		}

		propertyListingCount = new Integer(propertyCount).intValue();
		numberOfPages = Math.round(Math.ceil((double) propertyListingCount
				/ (double) PROPERTIES_PER_PAGE));

		for (int index = 1; index <= numberOfPages;) {

			// First step - find each property description in the web page.
			index1 = buffer.indexOf(string1);
			while (index1 > 0) {
				index2 = buffer.indexOf(string2, index1);
				details = buffer.substring(index1, index2);

				index3 = details.indexOf(string3);
				index4 = details.indexOf(gt, index3);
				index5 = details.indexOf(string4, index4);
				price = details.substring(index4 + 1, index5);

				// Second step - find the price in the description.
				if (index4 > 0) {

					try {
						amount = nf.parse(price);

					} catch (ParseException pe) {
						// Skip any where they can't enter the price correctly
						index1 = buffer.indexOf(string1, index2);
						continue;
					}
				} else {
					break;
				}

				// Third step - find the number of bedrooms.
				index6 = details.indexOf(string5, index5);
				if (index6 > 0) {
					index7 = details.indexOf(gt, index6);
					bedrooms = details.substring(index7 + 1,
							details.indexOf(string4, index7));

					try {
						df.parse(bedrooms);
					} catch (ParseException pe) {
						// Skip any where they can't enter the bedrooms
						// correctly
						index1 = buffer.indexOf(string1, index2);
						continue;
					}
				} else {
					bedrooms = "0";
				}

				// Fourth step - find the number of bathrooms.
				index8 = details.indexOf(string6, index7);
				if (index8 > 0) {
					index9 = details.indexOf(gt, index8);
					bathrooms = details.substring(index9 + 1,
							details.indexOf(string4, index9));

					try {
						df.parse(bathrooms);
					} catch (ParseException pe) {
						// Skip any where they can't enter the bathrooms
						// correctly
						index1 = buffer.indexOf(string1, index2);
						continue;
					}
				} else {
					bathrooms = "0";
				}

				// Fifth step - find the property type.
				index10 = details.indexOf(string7, index5);
				if (index10 > 0) {
					index11 = details.indexOf(gt, index5);
					propertyType = details.substring(index11 + 1,
							details.indexOf(string4, index11));

					if (propertyType.indexOf("House") > 0) {
						propertyType = "House";
					} else {
						propertyType = "Unit";
					}
				}

				Property property = new Property(amount.toString(), bedrooms,
						bathrooms, propertyType);
				properties.add(property);

				index1 = buffer.indexOf(string1, index2);
			}

			if (type.equals("Buy")) {
				buffer = new StringBuffer(getWebpage(buyWebpageURL + ++index));
			} else {
				buffer = new StringBuffer(getWebpage(rentWebpageURL + ++index));
			}
		}

		return properties;
	}

	// Get the "buy/rent listing" page from domain.com.au.
	private static String getWebpage(String webpageURL) {
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

		try {

			URL server = new URL(webpageURL);
			HttpURLConnection serverConnection = (HttpURLConnection) server
					.openConnection();
			serverConnection.connect();

			int httpResponse = serverConnection.getResponseCode();

			if (httpResponse == HttpURLConnection.HTTP_OK) {

				BufferedInputStream in = new BufferedInputStream(
						serverConnection.getInputStream());
				int c;

				while ((c = in.read()) != -1) {
					byteArrayOut.write(c);
				}
			} else {
				System.out.println("HTTP Error: " + httpResponse);
			}

			serverConnection.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return byteArrayOut.toString();
	}

	// Calculate average (mean) house price for # bedroom properties.
	private static double calculateAveragePrice(List<Property> properties,
			int bedroomCount, String propertyType) {
		double sum = 0f;
		int count = 0;

		Iterator propertyList = properties.iterator();
		while (propertyList.hasNext()) {
			Property property = (Property) propertyList.next();
			if (new Integer(property.getBedrooms()).equals(bedroomCount)
					&& propertyType.equals(property.getPropertyType())) {
				sum += new Double(property.getPrice());
				count++;
			}
		}

		return sum / count;
	}

	private static void createSummaryTable() {
		Connection conn = null;
		Statement stmt = null;
		String createTable = "CREATE TABLE Yield (Town TEXT, Type TEXT, \"Mean Price\" TEXT, \"Mean Rent\" TEXT, Yield TEXT);";

		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			conn = DriverManager.getConnection("jdbc:odbc:rentalyield", "", "");

			stmt = conn.createStatement();
			stmt.executeUpdate(createTable);

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	private static void createTable(String tableName) {
		Connection conn = null;
		Statement stmt = null;
		String createTable = "CREATE TABLE \""
				+ tableName
				+ "\" (\"Buy or Rent\" TEXT, Type TEXT, Bedrooms TEXT, Bathrooms TEXT, Purchase TEXT, Rent TEXT, Mean TEXT, Variance TEXT)";

		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			conn = DriverManager.getConnection("jdbc:odbc:rentalyield", "", "");

			stmt = conn.createStatement();
			stmt.executeUpdate(createTable);

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	// We're going to group entries according to bedroom numbers.
	private static void writeToSpreadsheet(List<Property> properties,
			String buyOrRent, String town) {

		NumberFormat yieldFormat = new DecimalFormat("##.##");
		Connection conn = null;
		Statement stmt = null;

		String insertBuyOrRentHeader = "INSERT INTO ["
				+ town.replaceAll(" ", "_") + "$] (\"Buy or Rent\") VALUES ('"
				+ buyOrRent + "');";
		String insertBedroomCountHeader = "INSERT INTO ["
				+ town.replaceAll(" ", "_") + "$] (Type) VALUES ('";
		String insertProperty = "INSERT INTO [" + town.replaceAll(" ", "_")
				+ "$] (Bedrooms, Bathrooms, Purchase) VALUES ('";
		String insertMean = "INSERT INTO [" + town.replaceAll(" ", "_")
				+ "$] (Mean, Variance) VALUES ('";
		String insertBlank = "INSERT INTO [" + town.replaceAll(" ", "_")
				+ "$] (\"Buy or Rent\") VALUES (' ');";
		String insertSummary = "INSERT INTO [Yield$] (Town, Type, \"Mean Price\", \"Mean Rent\", Yield) VALUES ('";

		if (buyOrRent.equals("Rent")) {
			insertProperty = "INSERT INTO [" + town.replaceAll(" ", "_")
					+ "$] (Bedrooms, Bathrooms, Rent) VALUES ('";
		}

		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			conn = DriverManager.getConnection("jdbc:odbc:rentalyield", "", "");

			stmt = conn.createStatement();
			stmt.executeUpdate(insertBuyOrRentHeader);

			for (int index = 0; index < 2; index++) {

				int bedroomCount = 1;
				String propertyType = "House";

				if (index == 1)
					propertyType = "Unit";

				stmt.executeUpdate(insertBedroomCountHeader + propertyType
						+ "');");

				for (; bedroomCount <= MAX_BEDROOMS;) {
					boolean hasProperties = false;
					maxHousePurchaseValue = new Hashtable<Integer, Double>();
					minHousePurchaseValue = new Hashtable<Integer, Double>();
					maxUnitPurchaseValue = new Hashtable<Integer, Double>();
					minUnitPurchaseValue = new Hashtable<Integer, Double>();
					maxHouseRentValue = new Hashtable<Integer, Double>();
					minHouseRentValue = new Hashtable<Integer, Double>();
					maxUnitRentValue = new Hashtable<Integer, Double>();
					minUnitRentValue = new Hashtable<Integer, Double>();

					Iterator propertyList = properties.iterator();
					double average = calculateAveragePrice(properties,
							bedroomCount, propertyType);

					// Insert each property into sheet based on # bedrooms and
					// property type.
					while (propertyList.hasNext()) {
						Property property = (Property) propertyList.next();
						if (new Integer(property.getBedrooms())
								.equals(bedroomCount)
								&& property.getPropertyType().equals(
										propertyType)) {
							hasProperties = true;
							String insertParams = property.getBedrooms()
									+ "','"
									+ property.getBathrooms()
									+ "','"
									+ nf.format(new Double(property.getPrice()))
									+ "')";
							stmt.executeUpdate(insertProperty + insertParams);

							if (buyOrRent.equals("Buy")) {
								if (propertyType.equals("House")) {
									putMinMax(minHousePurchaseValue,
											maxHousePurchaseValue,
											bedroomCount,
											new Double(property.getPrice()));
								} else {
									putMinMax(minUnitPurchaseValue,
											maxUnitPurchaseValue, bedroomCount,
											new Double(property.getPrice()));
								}
							} else {
								if (propertyType.equals("House")) {
									putMinMax(minHouseRentValue,
											maxHouseRentValue, bedroomCount,
											new Double(property.getPrice()));
								} else {
									putMinMax(minUnitRentValue,
											maxUnitRentValue, bedroomCount,
											new Double(property.getPrice()));
								}
							}
						}
					}

					// If there are # bedroom properties, calculate averages and
					// min/max.
					if (hasProperties) {
						if (buyOrRent.equals("Rent")) {
							if (propertyType.equals("House")) {
								meanHouseRentValues.put(new Integer(
										bedroomCount), new Double(average));

								double variance = 0.0;
								if (meanHouseRentValues.get(bedroomCount) != null) {
									variance = calculateVariance(
											minHouseRentValue,
											maxHouseRentValue, bedroomCount);
									stmt.executeUpdate(insertMean
											+ nf.format(new Double(average))
											+ "', '"
											+ yieldFormat.format(new Double(
													variance)) + "');");
								}

								double rentalYield = 0.0;
								if (meanHousePurchaseValues.get(bedroomCount) != null) {
									rentalYield = (average * 52)
											/ meanHousePurchaseValues
													.get(bedroomCount);
									stmt.executeUpdate(insertSummary
											+ town
											+ "','"
											+ bedroomCount
											+ " Bedroom "
											+ propertyType
											+ "','"
											+ nf.format(new Double(
													meanHousePurchaseValues
															.get(bedroomCount)))
											+ "','"
											+ nf.format(new Double(average))
											+ "','"
											+ yieldFormat
													.format(rentalYield * 100)
											+ "');");
								}
							} else if (propertyType.equals("Unit")) {
								meanUnitRentValues.put(
										new Integer(bedroomCount), new Double(
												average));

								double variance = 0.0;
								if (meanUnitRentValues.get(bedroomCount) != null) {
									variance = calculateVariance(
											minUnitRentValue, maxUnitRentValue,
											bedroomCount);
									stmt.executeUpdate(insertMean
											+ nf.format(new Double(average))
											+ "', '"
											+ yieldFormat.format(new Double(
													variance)) + "');");
								}

								double rentalYield = 0.0;
								if (meanUnitPurchaseValues.get(bedroomCount) != null) {
									rentalYield = (average * 52)
											/ meanUnitPurchaseValues
													.get(bedroomCount);
									stmt.executeUpdate(insertSummary
											+ town
											+ "','"
											+ bedroomCount
											+ " Bedroom "
											+ propertyType
											+ "','"
											+ nf.format(new Double(
													meanUnitPurchaseValues
															.get(bedroomCount)))
											+ "','"
											+ nf.format(new Double(average))
											+ "','"
											+ yieldFormat
													.format(rentalYield * 100)
											+ "');");
								}
							}
						}

						if (buyOrRent.equals("Buy")) {
							if (propertyType.equals("House")) {
								double variance = calculateVariance(
										minHousePurchaseValue,
										maxHousePurchaseValue, bedroomCount);
								stmt.executeUpdate(insertMean
										+ nf.format(new Double(average))
										+ "', '"
										+ yieldFormat.format(new Double(
												variance)) + "');");

								meanHousePurchaseValues.put(new Integer(
										bedroomCount), new Double(average));
							} else {
								double variance = calculateVariance(
										minUnitPurchaseValue,
										maxUnitPurchaseValue, bedroomCount);
								stmt.executeUpdate(insertMean
										+ nf.format(new Double(average))
										+ "', '"
										+ yieldFormat.format(new Double(
												variance)) + "');");

								meanUnitPurchaseValues.put(new Integer(
										bedroomCount), new Double(average));
							}
						}

						stmt.executeUpdate(insertBlank);
					}

					bedroomCount++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static private double calculateVariance(
			Hashtable<Integer, Double> minHouseValue,
			Hashtable<Integer, Double> maxHouseValue, int bedroomCount) {
		double variance = 0.0;
		if (minHouseValue.get(bedroomCount) == null
				|| maxHouseValue.get(bedroomCount) == null) {
			return variance;
		}

		double min = minHouseValue.get(bedroomCount);
		double max = maxHouseValue.get(bedroomCount);
		variance = (max - min) / min * 100;

		return variance;
	}

	static private void putMinMax(Hashtable<Integer, Double> minHouseValue,
			Hashtable<Integer, Double> maxHouseValue, int bedroomCount,
			Double price) {
		Double currentMin = minHouseValue.get(new Integer(bedroomCount));
		Double currentMax = maxHouseValue.get(new Integer(bedroomCount));

		if (currentMin == null) {
			minHouseValue.put(new Integer(bedroomCount), price);
		} else {
			if (price < currentMin) {
				minHouseValue.put(new Integer(bedroomCount), price);
			}
		}

		if (currentMax == null) {
			maxHouseValue.put(new Integer(bedroomCount), price);
		} else {
			if (price > currentMax) {
				maxHouseValue.put(new Integer(bedroomCount), price);
			}
		}
	}

	private static class Property {

		private String price;
		private String bedrooms;
		private String bathrooms;
		private String propertyType;

		public Property(String price, String bedrooms, String bathrooms,
				String propertyType) {
			this.price = price;
			this.bedrooms = bedrooms;
			this.bathrooms = bathrooms;
			this.propertyType = propertyType;
		}

		public String getPrice() {
			return price;
		}

		public String getBedrooms() {
			return bedrooms;
		}

		public String getBathrooms() {
			return bathrooms;
		}

		public String getPropertyType() {
			return propertyType;
		}
	}

	static private String[] suburbList = {

	"ABBOTSFORD", "ABERFELDIE", "ACHERON", "ADDINGTON", "AIREYS INLET",
			"AIRPORT WEST", "ALBANVALE", "ALBERT PARK", "ALBERTON",
			"ALBERTON WEST", "ALBION", "ALEXANDRA", "ALFREDTON", "ALLANS FLAT",
			"ALLANSFORD", "ALLESTREE", "ALMA", "ALMONDS", "ALMURTA",
			"ALPHINGTON", "ALTONA", "ALTONA MEADOWS", "ALTONA NORTH", "ALVIE",
			"AMPHITHEATRE", "ANAKIE", "ANDERSON", "ANGLERS REST", "ANGLESEA",
			"ANTWERP", "APOLLO BAY", "APPIN", "APSLEY", "ARARAT",
			"ARARAT EAST", "ARCADIA", "ARCHIES CREEK", "ARDEER", "ARDMONA",
			"AREEGRA", "ARMADALE", "ARMSTRONG", "ARTHURS CREEK",
			"ARTHURS SEAT", "ASCOT", "ASCOT VALE", "ASHBURTON", "ASHWOOD",
			"ASPENDALE", "ASPENDALE GARDENS", "ATTWOOD", "AVENEL", "AVOCA",
			"AVONDALE HEIGHTS", "AVONSLEIGH", "AXE CREEK", "AXEDALE",
			"BACCHUS MARSH", "BADDAGINNIE", "BAIRNSDALE", "BALACLAVA",
			"BALINTORE", "BALLAN", "BALLARAT", "BALLARAT CENTRAL",
			"BALLARAT EAST", "BALLARAT NORTH", "BALLIANG", "BALLIANG EAST",
			"BALNARRING", "BALNARRING BEACH", "BALOOK", "BALWYN",
			"BALWYN NORTH", "BAMAWM", "BAMBRA", "BANDIANA", "BANGHOLME",
			"BANNOCKBURN", "BANYENA", "BARANDUDA", "BARFOLD", "BARINGHUP",
			"BARKERS CREEK", "BARKSTEAD", "BARNADOWN", "BARNAWARTHA",
			"BARNAWARTHA NORTH", "BARONGAROOK", "BARRYS REEF", "BARWITE",
			"BARWON DOWNS", "BARWON HEADS", "BATESFORD", "BAW BAW", "BAXTER",
			"BAYLES", "BAYSWATER NORTH", "BEACONSFIELD", "BEACONSFIELD UPPER",
			"BEALIBA", "BEAUFORT", "BEAUMARIS", "BEEAC", "BEECH FOREST",
			"BEECHWORTH", "BELGRAVE", "BELGRAVE HEIGHTS", "BELGRAVE SOUTH",
			"BELL PARK", "BELL POST HILL", "BELLARINE", "BELLBRAE",
			"BELLBRIDGE", "BELLFIELD", "BELMONT", "BEMM RIVER", "BENA",
			"BENALLA", "BENAMBRA", "BEND OF ISLANDS", "BENDIGO", "BENDOC",
			"BENGWORDEN", "BENLOCH", "BENTLEIGH", "BENTLEIGH EAST", "BERRINGA",
			"BERRIWILLOCK", "BERRYS CREEK", "BERWICK", "BESSIEBELLE",
			"BET BET", "BETHANGA", "BEULAH", "BEVERIDGE", "BIG HILL",
			"BINGINWARRI", "BIRCHIP", "BIRDWOODTON", "BIRREGURRA", "BITTERN",
			"BLACK HILL", "BLACK ROCK", "BLACKBURN", "BLACKBURN NORTH",
			"BLACKBURN SOUTH", "BLACKWOOD", "BLAIRGOWRIE", "BLAKEVILLE",
			"BLAMPIED", "BLIND BIGHT", "BOBINAWARRAH", "BOGONG", "BOISDALE",
			"BOLWARRA", "BONANG", "BONBEACH", "BONEO", "BONNIE DOON",
			"BOOLARRA", "BOORHAMAN", "BOORT", "BOOSEY", "BORALMA", "BORONIA",
			"BOX HILL", "BOX HILL CENTRAL", "BOX HILL NORTH", "BOX HILL SOUTH",
			"BRANDY CREEK", "BRANXHOLME", "BRAYBROOK", "BREAKWATER",
			"BRIAGOLONG", "BRIAR HILL", "BRIDGE CREEK", "BRIDGEWATER",
			"BRIDGEWATER ON LODDON", "BRIGHT", "BRIGHTON", "BRIGHTON EAST",
			"BRIM", "BROADFORD", "BROADMEADOWS", "BROADWATER", "BROOKFIELD",
			"BROOKLYN", "BROOMFIELD", "BROWN HILL", "BRUCKNELL", "BRUNSWICK",
			"BRUNSWICK EAST", "BRUNSWICK WEST", "BRUTHEN", "BUANGOR", "BUCHAN",
			"BUCKRABANYULE", "BUDGEREE", "BUFFALO", "BULLA", "BULLEEN",
			"BULLENGAROOK", "BULN BULN", "BUNBARTHA", "BUNDALAGUAH",
			"BUNDALONG", "BUNDOORA", "BUNINYONG", "BUNYIP", "BUNYIP NORTH",
			"BURNSIDE", "BURNSIDE HEIGHTS", "BURNT CREEK", "BURRAMINE",
			"BURRUMBEET", "BURWOOD", "BURWOOD EAST", "BUSHFIELD", "BUXTON",
			"BYADUK NORTH", "BYRNESIDE", "CABARITA", "CABBAGE TREE CREEK",
			"CAIRNLEA", "CALIFORNIA GULLY", "CALLAWADDA", "CALULU",
			"CAMBERWELL", "CAMPBELLFIELD", "CAMPBELLS CREEK", "CAMPERDOWN",
			"CANADIAN", "CANN RIVER", "CANNONS CREEK", "CANTERBURY",
			"CAPE BRIDGEWATER", "CAPE CLEAR", "CAPE OTWAY", "CAPE PATERSON",
			"CAPE SCHANCK", "CAPE WOOLAMAI", "CARAMUT", "CARDIGAN",
			"CARDIGAN VILLAGE", "CARDINIA", "CARDROSS", "CARGERIE",
			"CARISBROOK", "CARLISLE RIVER", "CARLSRUHE", "CARLTON",
			"CARLTON NORTH", "CARNEGIE", "CAROLINE SPRINGS", "CARRAJUNG",
			"CARRUM", "CARRUM DOWNS", "CASSILIS", "CASTERTON",
			"CASTLE DONNINGTON", "CASTLEMAINE", "CATANI", "CAULFIELD",
			"CAULFIELD EAST", "CAULFIELD NORTH", "CAULFIELD SOUTH",
			"CAVENDISH", "CERES", "CHADSTONE", "CHARLTON", "CHELSEA",
			"CHELSEA HEIGHTS", "CHELTENHAM", "CHESHUNT", "CHESNEY VALE",
			"CHEWTON", "CHILDERS", "CHILTERN", "CHINKAPOOK", "CHINTIN",
			"CHIRNSIDE PARK", "CHUM CREEK", "CHURCHILL", "CLARINDA",
			"CLARKEFIELD", "CLARKES HILL", "CLAYTON", "CLAYTON NORTH",
			"CLAYTON SOUTH", "CLEMATIS", "CLIFTON CREEK", "CLIFTON HILL",
			"CLIFTON SPRINGS", "CLONBINANE", "CLOVERLEA", "CLUB TERRACE",
			"CLUNES", "CLYDE", "CLYDE NORTH", "CLYDEBANK", "COALVILLE",
			"COBDEN", "COBRAM", "COBRAM EAST", "COBUNGRA", "COBURG",
			"COBURG EAST", "COBURG NORTH", "COCKATOO", "COHUNA", "COIMADAI",
			"COLAC", "COLAC EAST", "COLBINABBIN", "COLDSTREAM", "COLERAINE",
			"COLIGNAN", "COLLINGWOOD", "CONDAH", "CONGUPNA", "CONNEWARRE",
			"COOLAROO", "COOMA", "COOMOORA", "COONGULLA", "COONOOER BRIDGE",
			"COORIEMUNGLE", "CORA LYNN", "CORAGULAC", "CORINDHAP", "CORINELLA",
			"CORIO", "CORNELLA", "CORONET BAY", "COROP", "COROROOKE",
			"CORRYONG", "CORUNNUN", "COSTERFIELD", "COTTLES BRIDGE", "COWES",
			"COWWARR", "CRAIGIEBURN", "CRANBOURNE", "CRANBOURNE EAST",
			"CRANBOURNE NORTH", "CRANBOURNE SOUTH", "CRANBOURNE WEST",
			"CREIGHTONS CREEK", "CRESSY", "CRESWICK", "CRIB POINT",
			"CROWLANDS", "CROYDON", "CROYDON HILLS", "CROYDON NORTH",
			"CROYDON SOUTH", "CUDGEE", "CUDGEWA", "CURLEWIS", "DAISY HILL",
			"DALES CREEK", "DALLAS", "DALMORE", "DALYSTON", "DANDENONG",
			"DANDENONG NORTH", "DANDENONG SOUTH", "DARGO", "DARLEY", "DARNUM",
			"DARRAWEIT GUIM", "DARRIMAN", "DARTMOOR", "DARTMOUTH",
			"DAYLESFORD", "DEANS MARSH", "DEDERANG", "DEEP LEAD", "DEER PARK",
			"DELACOMBE", "DELAHEY", "DENISON", "DENNINGTON", "DEREEL",
			"DERRIMUT", "DERRINAL", "DERRINALLUM", "DEVENISH", "DEVON MEADOWS",
			"DEVON NORTH", "DIAMOND CREEK", "DIGBY", "DIGGERS REST",
			"DIMBOOLA", "DINGEE", "DINGLEY VILLAGE", "DINNER PLAIN", "DIXIE",
			"DIXONS CREEK", "DOCKLANDS", "DOLLAR", "DON VALLEY", "DONALD",
			"DONCASTER", "DONCASTER EAST", "DONVALE", "DOOKIE", "DOREEN",
			"DOVETON", "DROMANA", "DROUIN", "DROUIN EAST", "DROUIN SOUTH",
			"DRUMCONDRA", "DRUMMOND", "DRUMMOND NORTH", "DRYSDALE", "DUMBALK",
			"DUNDONNELL", "DUNKELD", "DUNNSTOWN", "DUNOLLY", "DURHAM LEAD",
			"DURHAM OX", "DUTTON WAY", "EAGLE POINT", "EAGLEHAWK", "EAGLEMONT",
			"EAST BENDIGO", "EAST GEELONG", "EAST MELBOURNE", "EAST WARBURTON",
			"EASTERN VIEW", "EBDEN", "ECHUCA", "ECHUCA WEST", "ECKLIN SOUTH",
			"EDEN PARK", "EDENHOPE", "EDITHVALE", "EGANSTOWN", "EILDON",
			"ELAINE", "ELDORADO", "ELINGAMITE", "ELINGAMITE NORTH",
			"ELLERSLIE", "ELLIMINYT", "ELMHURST", "ELMORE", "ELPHINSTONE",
			"ELSTERNWICK", "ELTHAM", "ELTHAM NORTH", "ELWOOD", "EMERALD",
			"EMU", "ENDEAVOUR HILLS", "ENFIELD", "ENSAY", "EPPALOCK", "EPPING",
			"EPSOM", "ERICA", "ESSENDON", "ESSENDON NORTH", "ESSENDON WEST",
			"EUMEMMERRING", "EUROA", "EUROBIN", "EXFORD", "EYNESBURY",
			"FAIRFIELD", "FAIRHAVEN", "FAIRY DELL", "FALLS CREEK", "FARADAY",
			"FAWKNER", "FERNBANK", "FERNTREE GULLY", "FERNY CREEK", "FINGAL",
			"FISH CREEK", "FITZROY", "FITZROY NORTH", "FLAGGY CREEK",
			"FLEMINGTON", "FLINDERS", "FLORA HILL", "FLOWERDALE", "FLYNN",
			"FOOTSCRAY", "FOREST HILL", "FORGE CREEK", "FORREST", "FOSTER",
			"FOSTER NORTH", "FOUNTAIN GATE", "FRAMLINGHAM", "FRANKLINFORD",
			"FRANKSTON", "FRANKSTON NORTH", "FRANKSTON SOUTH", "FREEBURGH",
			"FRENCH ISLAND", "FRESHWATER CREEK", "FRYERSTOWN", "GARFIELD",
			"GARIBALDI", "GARVOC", "GEELONG", "GEELONG WEST", "GELLIBRAND",
			"GEMBROOK", "GENOA", "GHERANG", "GHERINGHAP", "GIFFARD WEST",
			"GILDEROY", "GIPSY POINT", "GIRGARRE", "GISBORNE",
			"GISBORNE SOUTH", "GLADSTONE PARK", "GLADYSDALE", "GLEN ALVIE",
			"GLEN FORBES", "GLEN HUNTLY", "GLEN IRIS", "GLEN WAVERLEY",
			"GLENAIRE", "GLENALADALE", "GLENBURN", "GLENFYNE", "GLENGARRY",
			"GLENGARRY NORTH", "GLENGARRY WEST", "GLENHOPE", "GLENLYON",
			"GLENMAGGIE", "GLENORCHY", "GLENORMISTON NORTH", "GLENROWAN",
			"GLENROY", "GLENTHOMPSON", "GLOMAR BEACH", "GNARWARRE", "GOBUR",
			"GOLDEN BEACH", "GOLDEN POINT", "GOLDEN SQUARE", "GOONGERAH",
			"GOORAMBAT", "GOORNONG", "GORAE", "GORAE WEST", "GORDON",
			"GORMANDALE", "GOROKE", "GOUGHS BAY", "GOULBURN WEIR", "GOWANBRAE",
			"GRAHAMVALE", "GRANITE ROCK", "GRANTVILLE", "GRAYTOWN",
			"GREAT WESTERN", "GREENDALE", "GREENSBOROUGH", "GREENVALE",
			"GROVEDALE", "GRUYERE", "GUILDFORD", "GUNBOWER", "GUYS HILL",
			"HADDON", "HADFIELD", "HALLAM", "HALLS GAP", "HALLSTON",
			"HAMILTON", "HAMLYN HEIGHTS", "HAMPTON", "HAMPTON EAST",
			"HAMPTON PARK", "HANGING ROCK", "HARCOURT", "HARCOURT NORTH",
			"HARKAWAY", "HARMERS HAVEN", "HARRIETVILLE", "HARROW", "HASTINGS",
			"HAVEN", "HAWKESDALE", "HAWTHORN", "HAWTHORN EAST",
			"HAZELWOOD NORTH", "HAZELWOOD SOUTH", "HEALESVILLE", "HEATH HILL",
			"HEATHCOTE", "HEATHCOTE JUNCTION", "HEATHCOTE SOUTH", "HEATHERTON",
			"HEATHMERE", "HEATHMONT", "HEDLEY", "HEIDELBERG",
			"HEIDELBERG HEIGHTS", "HEIDELBERG WEST", "HENTY",
			"HEPBURN SPRINGS", "HERNE HILL", "HERNES OAK", "HESKET", "HEXHAM",
			"HEYFIELD", "HEYWOOD", "HIDDEN VALLEY", "HIGHETT", "HIGHTON",
			"HILL END", "HILLCREST", "HILLSIDE", "HODDLES CREEK",
			"HOLLANDS LANDING", "HOMEWOOD", "HOPETOUN", "HOPETOUN PARK",
			"HOPPERS CROSSING", "HORDERN VALE", "HORSHAM", "HOWQUA",
			"HOWQUA INLET", "HUGHESDALE", "HUNTER", "HUNTINGDALE", "HUNTLY",
			"HURSTBRIDGE", "ILLOWA", "INDENTED HEAD", "INGLEWOOD",
			"INVERGORDON", "INVERLEIGH", "INVERLOCH", "INVERMAY",
			"INVERMAY PARK", "IRAAK", "IRISHTOWN", "IRONBARK", "IRREWARRA",
			"IRREWILLIPE", "IRYMPLE", "IVANHOE", "IVANHOE EAST", "JACANA",
			"JACK RIVER", "JACKASS FLAT", "JAM JERRUP", "JAMIESON", "JAN JUC",
			"JANCOURT EAST", "JEERALANG JUNCTION", "JEETHO", "JEPARIT",
			"JINDIVICK", "JOHANNA", "JOHNSONVILLE", "JUNCTION VILLAGE",
			"JUNORTOUN", "KALIMNA", "KALKALLO", "KALLISTA", "KALORAMA",
			"KANGAROO FLAT", "KANGAROO GROUND", "KANGAROO LAKE", "KANIVA",
			"KANUMBRA", "KARDELLA SOUTH", "KARNAK", "KATAMATITE", "KATANDRA",
			"KATANDRA WEST", "KATUNGA", "KAWARREN", "KEALBA", "KEILOR",
			"KEILOR DOWNS", "KEILOR EAST", "KEILOR LODGE", "KEILOR NORTH",
			"KEILOR PARK", "KENNEDYS CREEK", "KENNETT RIVER", "KENNINGTON",
			"KENSINGTON", "KERANG", "KERGUNYAH", "KERNOT", "KEVINGTON", "KEW",
			"KEW EAST", "KEYSBOROUGH", "KIALLA", "KIALLA LAKES", "KIALLA WEST",
			"KIEWA", "KILCUNDA", "KILLARA", "KILLARNEY", "KILMANY", "KILMORE",
			"KILMORE EAST", "KILSYTH", "KILSYTH SOUTH", "KINGLAKE",
			"KINGLAKE WEST", "KINGS PARK", "KINGSBURY", "KINGSVILLE",
			"KNOWSLEY", "KNOXFIELD", "KONGWAK", "KOO WEE RUP", "KOONDROOK",
			"KOONOOMOO", "KOONWARRA", "KOORLONG", "KOORNALLA", "KOOYONG",
			"KOROIT", "KORONG VALE", "KORUMBURRA", "KORUMBURRA SOUTH",
			"KORWEINGUBOORA", "KOTTA", "KOYUGA", "KROWERA", "KURUNJANG",
			"KYABRAM", "KYNETON", "LAANECOORIE", "LAANG", "LABERTOUCHE",
			"LAEN", "LAHARUM", "LAKE BOLAC", "LAKE BUNGA", "LAKE CHARM",
			"LAKE EILDON", "LAKE GARDENS", "LAKE MOKOAN", "LAKE ROWAN",
			"LAKE TYERS BEACH", "LAKE WENDOUREE", "LAKES ENTRANCE", "LAL LAL",
			"LALBERT", "LALOR", "LAMPLOUGH", "LANCASTER", "LANCE CREEK",
			"LANCEFIELD", "LANDSBOROUGH", "LANG LANG", "LANG LANG EAST",
			"LANGSBOROUGH", "LANGWARRIN", "LANGWARRIN SOUTH", "LARA",
			"LAUNCHING PLACE", "LAVERS HILL", "LAVERTON", "LEARMONTH",
			"LEITCHVILLE", "LEMNOS", "LENEVA", "LEONGATHA", "LEONGATHA NORTH",
			"LEONGATHA SOUTH", "LEOPOLD", "LETHBRIDGE", "LEXTON", "LILLICUR",
			"LILLIMUR", "LILYDALE", "LIMA", "LIMA EAST", "LIMA SOUTH",
			"LINDENOW", "LINDENOW SOUTH", "LINTON", "LISMORE", "LITTLE RIVER",
			"LLANELLY", "LOCH", "LOCH SPORT", "LOCKINGTON", "LOCKWOOD",
			"LOCKWOOD SOUTH", "LONG FOREST", "LONG GULLY", "LONGFORD",
			"LONGLEA", "LONGWARRY", "LONGWARRY NORTH", "LONGWOOD", "LORNE",
			"LOVELY BANKS", "LOWER PLENTY", "LUCKNOW", "LYNBROOK", "LYNDHURST",
			"LYONS", "LYONVILLE", "LYSTERFIELD", "LYSTERFIELD SOUTH",
			"MACARTHUR", "MACCLESFIELD", "MACEDON", "MACLEOD", "MACS COVE",
			"MADDINGLEY", "MAFFRA", "MAFFRA WEST UPPER", "MAIDEN GULLY",
			"MAIDSTONE", "MAILORS FLAT", "MAIN RIDGE", "MAINDAMPLE", "MAJORCA",
			"MALDON", "MALLACOOTA", "MALMSBURY", "MALVERN", "MALVERN EAST",
			"MANANGATANG", "MANDURANG", "MANDURANG SOUTH", "MANIFOLD HEIGHTS",
			"MANNS BEACH", "MANSFIELD", "MARDAN", "MARENGO", "MARIBYRNONG",
			"MARLO", "MARNOO", "MARONG", "MARSHALL", "MARYBOROUGH",
			"MARYKNOLL", "MCCRAE", "MCKINNON", "MCLOUGHLINS BEACH",
			"MCMAHONS CREEK", "MEADOW HEIGHTS", "MEENIYAN", "MEERLIEU",
			"MELBOURNE", "MELTON", "MELTON SOUTH", "MELTON WEST", "MENTONE",
			"MENZIES CREEK", "MERBEIN", "MERBEIN SOUTH", "MEREDITH",
			"MERINGUR", "MERNDA", "MERRICKS", "MERRICKS BEACH",
			"MERRICKS NORTH", "MERRIGUM", "MERRIJIG", "MERTON", "METCALFE",
			"METCALFE EAST", "METUNG", "MIA MIA", "MICKLEHAM", "MIDDLE PARK",
			"MIEPOLL", "MILAWA", "MILDURA", "MILDURA SOUTH", "MILL PARK",
			"MILLBROOK", "MILLGROVE", "MINCHA", "MINERS REST", "MINHAMITE",
			"MINYIP", "MIRBOO", "MIRBOO NORTH", "MITCHAM", "MITCHAM NORTH",
			"MITCHELL PARK", "MITTA MITTA", "MODELLA", "MOE", "MOGGS CREEK",
			"MOLIAGUL", "MOLLONGGHIP", "MOLYULLAH", "MONBULK", "MONEGEETTA",
			"MONT ALBERT", "MONT ALBERT NORTH", "MONTGOMERY", "MONTMORENCY",
			"MONTROSE", "MOOLAP", "MOONAMBEL", "MOONEE PONDS", "MOORABBIN",
			"MOORABOOL", "MOOROODUC", "MOOROOLBARK", "MOOROOPNA", "MORDIALLOC",
			"MORIAC", "MORNINGTON", "MORRISONS", "MORRL MORRL", "MORTLAKE",
			"MORWELL", "MOUNT BEAUTY", "MOUNT BULLER", "MOUNT BURNETT",
			"MOUNT CAMEL", "MOUNT CLEAR", "MOUNT DANDENONG", "MOUNT DORAN",
			"MOUNT DUNEED", "MOUNT ECCLES", "MOUNT EGERTON", "MOUNT ELIZA",
			"MOUNT EVELYN", "MOUNT HELEN", "MOUNT HOOGHLY", "MOUNT HOTHAM",
			"MOUNT MACEDON", "MOUNT MARTHA", "MOUNT PLEASANT",
			"MOUNT PROSPECT", "MOUNT RICHMOND", "MOUNT WALLACE",
			"MOUNT WAVERLEY", "MOUNTAIN BAY", "MOYARRA", "MOYHU", "MOYSTON",
			"MT BAW BAW", "MUCKLEFORD", "MULGRAVE", "MURCHISON", "MURRABIT",
			"MURRINDINDI", "MURROON", "MURRUMBEENA", "MURTOA", "MUSK VALE",
			"MYERS FLAT", "MYRNIONG", "MYRTLEBANK", "MYRTLEFORD", "MYSIA",
			"NAGAMBIE", "NALANGIL", "NANGILOC", "NANNEELLA", "NAPOLEONS",
			"NAR NAR GOON", "NAR NAR GOON NORTH", "NARBETHONG", "NARING",
			"NARINGAL", "NARRAWONG", "NARRE WARREN", "NARRE WARREN EAST",
			"NARRE WARREN NORTH", "NARRE WARREN SOUTH", "NATHALIA", "NATIMUK",
			"NATTE YALLOCK", "NAVARRE", "NEERIM", "NEERIM SOUTH",
			"NEILBOROUGH", "NELSON", "NERRENA", "NERRINA", "NEW GISBORNE",
			"NEWBOROUGH", "NEWBRIDGE", "NEWBURY", "NEWCOMB", "NEWHAM",
			"NEWHAVEN", "NEWINGTON", "NEWLANDS ARM", "NEWLYN", "NEWPORT",
			"NEWRY", "NEWSTEAD", "NEWTOWN", "NHILL", "NICHOLS POINT",
			"NICHOLSON", "NIDDRIE", "NILMA", "NINTINGBOOL", "NOBLE PARK",
			"NOBLE PARK NORTH", "NOOJEE", "NOORAT", "NOORAT EAST", "NORLANE",
			"NORTH BENDIGO", "NORTH GEELONG", "NORTH MELBOURNE", "NORTH SHORE",
			"NORTHCOTE", "NOTTING HILL", "NOWA NOWA", "NUMURKAH", "NUNAWADING",
			"NUNGURNER", "NUTFIELD", "NYAH", "NYERIMILANG", "NYORA",
			"OAK PARK", "OAKLANDS JUNCTION", "OAKLEIGH", "OAKLEIGH EAST",
			"OAKLEIGH SOUTH", "OCEAN GRANGE", "OCEAN GROVE", "OFFICER",
			"OLINDA", "OLIVERS HILL", "OMEO", "ORBOST", "ORMOND", "OUTTRIM",
			"OUYEN", "OVENS", "OXLEY", "PAKENHAM", "PAKENHAM UPPER", "PANMURE",
			"PANTON HILL", "PARADISE", "PARADISE BEACH", "PARK ORCHARDS",
			"PARKDALE", "PARKVILLE", "PARWAN", "PASCOE VALE",
			"PASCOE VALE SOUTH", "PATTERSON LAKES", "PAYNESVILLE",
			"PEARCEDALE", "PEARSONDALE", "PEECHELBA", "PENNYROYAL",
			"PENSHURST", "PERRY BRIDGE", "PETERBOROUGH", "PHEASANT CREEK",
			"PICOLA", "PIONEER BAY", "PIPERS CREEK", "PIRRON YALLOCK",
			"PLENTY", "PLUMPTON", "POINT COOK", "POINT LEO", "POINT LONSDALE",
			"POMBORNEIT", "POMONAL", "POOWONG", "POOWONG NORTH",
			"PORCUPINE RIDGE", "POREPUNKAH", "PORT ALBERT", "PORT CAMPBELL",
			"PORT FAIRY", "PORT FRANKLIN", "PORT MELBOURNE", "PORT WELSHPOOL",
			"PORTARLINGTON", "PORTLAND", "PORTLAND WEST", "PORTSEA",
			"POUND CREEK", "POWELLTOWN", "PRAHRAN", "PRAHRAN EAST", "PRESTON",
			"PRESTON SOUTH", "PRESTON WEST", "PRINCETOWN", "PURNIM", "PYALONG",
			"PYRAMID HILL", "QUAMBATOOK", "QUANTONG", "QUARRY HILL",
			"QUEENSCLIFF", "RAGLAN", "RAINBOW", "RAVENSWOOD", "RAWSON",
			"RAYMOND ISLAND", "RAYWOOD", "RED CLIFFS", "RED HILL",
			"RED HILL SOUTH", "REDAN", "REDBANK", "REDESDALE", "REEFTON",
			"REGENT", "REGENT WEST", "RESEARCH", "RESERVOIR", "RHYLL",
			"RHYMNEY", "RICHMOND", "RIDDELLS CREEK", "RINGWOOD",
			"RINGWOOD EAST", "RINGWOOD NORTH", "RIPPLEBROOK", "RIPPLESIDE",
			"ROBERTSONS BEACH", "ROBINVALE", "ROCHESTER", "ROCKBANK", "ROKEBY",
			"ROKEWOOD", "ROMSEY", "ROSANNA", "ROSEBUD", "ROSEBUD SOUTH",
			"ROSEBUD WEST", "ROSEDALE", "ROSEWHITE", "ROSS CREEK", "ROWSLEY",
			"ROWVILLE", "ROXBURGH PARK", "RUBICON", "RUPANYUP", "RUSHWORTH",
			"RUTHERGLEN", "RYANSTON", "RYE", "SAFETY BEACH", "SAILORS GULLY",
			"SALE", "SAN REMO", "SANCTUARY LAKES", "SANDHURST", "SANDRINGHAM",
			"SANDY CREEK", "SANDY POINT", "SARSFIELD", "SASSAFRAS",
			"SAWMILL SETTLEMENT", "SCARSDALE", "SCORESBY", "SCOTSBURN",
			"SCOTTS CREEK", "SEA LAKE", "SEABROOK", "SEACOMBE", "SEAFORD",
			"SEAHOLME", "SEASPRAY", "SEATON", "SEAVIEW", "SEBASTIAN",
			"SEBASTOPOL", "SEDDON", "SEDGWICK", "SELBY", "SEPARATION CREEK",
			"SERPENTINE", "SEVILLE", "SEVILLE EAST", "SEYMOUR", "SHE OAKS",
			"SHELBOURNE", "SHELFORD", "SHEOAKS", "SHEPHERDS FLAT",
			"SHEPPARTON", "SHEPPARTON EAST", "SHERBROOKE", "SHOREHAM",
			"SILVAN", "SILVER CREEK", "SILVERLEAVES", "SIMPSON",
			"SKENES CREEK", "SKIPTON", "SKYE", "SMEATON", "SMITHS BEACH",
			"SMYTHES CREEK", "SMYTHESDALE", "SNAKE VALLEY", "SOLDIERS HILL",
			"SOMERS", "SOMERVILLE", "SORRENTO", "SOUTH DUDLEY",
			"SOUTH GEELONG", "SOUTH KINGSVILLE", "SOUTH MELBOURNE",
			"SOUTH MORANG", "SOUTH YARRA", "SOUTHBANK", "SPEED", "SPOTSWOOD",
			"SPRING GULLY", "SPRING HILL", "SPRINGHURST", "SPRINGMOUNT",
			"SPRINGVALE", "SPRINGVALE SOUTH", "ST ALBANS", "ST ALBANS PARK",
			"ST ANDREWS", "ST ANDREWS BEACH", "ST ARNAUD", "ST HELENA",
			"ST HELENS", "ST JAMES", "ST KILDA", "ST KILDA EAST",
			"ST KILDA ROAD", "ST KILDA WEST", "ST LEONARDS", "STAGHORN FLAT",
			"STANHOPE", "STANLEY", "STAWELL", "STEELS CREEK", "STONEYFORD",
			"STONY CREEK", "STRADBROKE", "STRATFORD", "STRATH CREEK",
			"STRATHBOGIE", "STRATHDALE", "STRATHFIELDSAYE", "STRATHMERTON",
			"STRATHMORE", "STRATHMORE HEIGHTS", "STREATHAM", "STRZELECKI",
			"STUART MILL", "SUGGAN BUGGAN", "SULKY", "SUNBURY",
			"SUNDERLAND BAY", "SUNNYCLIFFS", "SUNSET STRIP", "SUNSHINE",
			"SUNSHINE NORTH", "SUNSHINE WEST", "SURF BEACH", "SURREY HILLS",
			"SURREY HILLS NORTH", "SUTTON GRANGE", "SWAN HILL", "SWAN REACH",
			"SWANPOOL", "SWIFTS CREEK", "SYDENHAM", "TAGGERTY", "TALBOT",
			"TALLANGATTA", "TALLAROOK", "TALLYGAROOPNA", "TAMBO UPPER",
			"TAMBOON", "TANDARRA", "TANGAMBALANGA", "TANJIL", "TANJIL EAST",
			"TANJIL SOUTH", "TARADALE", "TARNAGULLA", "TARNEIT",
			"TARRA VALLEY", "TARRAVILLE", "TARRINGTON", "TARWIN",
			"TARWIN LOWER", "TATONG", "TATURA", "TAWONGA", "TAWONGA SOUTH",
			"TAYLORS HILL", "TAYLORS LAKES", "TECOMA", "TEESDALE",
			"TEMPLESTOWE", "TEMPLESTOWE LOWER", "TENBY POINT", "TERANG",
			"TERIP TERIP", "TETOORA ROAD", "THE BASIN", "THE GURDIES",
			"THE HONEYSUCKLES", "THE PATCH", "THOMASTOWN", "THORNBURY",
			"THORNTON", "THORPDALE", "THOUGLA", "THREE BRIDGES", "TIMBOON",
			"TIMMERING", "TINAMBA", "TOLMIE", "TONGALA", "TONGIO", "TONIMBUK",
			"TOOBORAC", "TOOLAMBA", "TOOLANGI", "TOOLERN VALE", "TOONGABBIE",
			"TOORA", "TOORA NORTH", "TOORADIN", "TOORAK", "TOORLOO ARM",
			"TOOTGAROOK", "TORQUAY", "TORRUMBARRY", "TOWER HILL", "TOWONG",
			"TRAFALGAR", "TRAFALGAR EAST", "TRARALGON", "TRARALGON SOUTH",
			"TRAVANCORE", "TRAWOOL", "TRENTHAM", "TRENTHAM EAST", "TRUGANINA",
			"TUBBUT", "TULLAMARINE", "TUNGAMAH", "TYAAK", "TYABB", "TYERS",
			"TYLDEN", "TYNONG", "TYNONG NORTH", "UNDERA",
			"UPPER FERNTREE GULLY", "UPPER LURG", "UPPER PLENTY", "UPWEY",
			"VALENCIA CREEK", "VENTNOR", "VENUS BAY", "VERMONT",
			"VERMONT SOUTH", "VIEWBANK", "VIOLET TOWN", "WAAIA", "WAHGUNYAH",
			"WALHALLA", "WALKERVILLE", "WALKERVILLE NORTH",
			"WALKERVILLE SOUTH", "WALLACE", "WALLAN", "WALLAN EAST",
			"WALLINGTON", "WALMER", "WANDANA HEIGHTS", "WANDILIGONG", "WANDIN",
			"WANDIN EAST", "WANDIN NORTH", "WANDO VALE", "WANDONG",
			"WANGANDARY", "WANGARATTA", "WANGARATTA SOUTH", "WANNON",
			"WANTIRNA", "WANTIRNA SOUTH", "WARANGA", "WARATAH BAY",
			"WARATAH NORTH", "WARBURTON", "WARNCOORT", "WARNEET",
			"WARRACKNABEAL", "WARRAGUL", "WARRAK", "WARRANDYTE",
			"WARRANDYTE NORTH", "WARRANDYTE SOUTH", "WARRANWOOD",
			"WARRENBAYNE", "WARRNAMBOOL", "WARTOOK", "WATCHEM", "WATERWAYS",
			"WATSONIA", "WATSONIA NORTH", "WATTLE GLEN", "WAUBRA",
			"WAURN PONDS", "WEDDERBURN", "WEERING", "WELSHMANS REEF",
			"WELSHPOOL", "WENDOUREE", "WENSLEYDALE", "WERONA", "WERRIBEE",
			"WERRIBEE SOUTH", "WERRIMULL", "WESBURN", "WEST CREEK",
			"WEST FOOTSCRAY", "WEST MELBOURNE", "WESTBURY", "WESTMEADOWS",
			"WESTMERE", "WHEATSHEAF", "WHEELERS HILL", "WHITE HILLS",
			"WHITFIELD", "WHITTINGTON", "WHITTLESEA", "WHOROULY SOUTH",
			"WILBY", "WILDWOOD", "WILLAURA", "WILLIAMS LANDING",
			"WILLIAMSTOWN", "WILLOW GROVE", "WILLUNG SOUTH",
			"WIMBLEDON HEIGHTS", "WINCHELSEA", "WINDERMERE", "WINDSOR",
			"WINSLOW", "WISELEIGH", "WODONGA", "WOLLERT", "WON WRON", "WONGA",
			"WONGA PARK", "WONGARRA", "WONTHAGGI", "WOOD WOOD", "WOODEND",
			"WOODSIDE", "WOODSIDE BEACH", "WOODSTOCK", "WOODVALE", "WOOLAMAI",
			"WOOLSHED", "WOOLSTHORPE", "WOOMELANG", "WOORAGEE", "WOORARRA",
			"WOORI YALLOCK", "WULGULMERANG", "WUNGHNU", "WURRUK", "WY YUNG",
			"WYCHEPROOF", "WYE RIVER", "WYNDHAM VALE", "WYUNA", "YABBA NORTH",
			"YACKANDANDAH", "YALCA", "YALLAMBIE", "YALLOURN NORTH", "YAMBUK",
			"YAN YEAN", "YANAKIE", "YANDOIT", "YAPEEN", "YARCK", "YARRA GLEN",
			"YARRA JUNCTION", "YARRAGON", "YARRAGON SOUTH", "YARRAM",
			"YARRAMBAT", "YARRAVILLE", "YARRAWONGA", "YARRAWONGA SOUTH",
			"YARROWEYAH", "YEA", "YELLINGBO", "YELTA", "YEO", "YEODENE",
			"YINNAR", "YINNAR SOUTH", "YUNDOOL", "YUULONG" };
}
