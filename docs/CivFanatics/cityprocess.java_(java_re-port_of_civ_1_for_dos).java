/*
 * This is a development version of a JCivED source file, reverse-engineered and
 * back-ported from CIV.EXE assembly to Java (from Sid Meier's Civilization for MS-DOS)
 * It rougly covers 70% of the "city screen" routine which processes city resource
 * production, renders the city screen, and manages city screen interaction (not
 * ported, i.e. the 30% remaining)
 * darkpandaman @ gmail.com - 14/10/2014
 */

package dd.civ.logic.port;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Iterator;

import dd.civ.data.font.FontUtils;
import dd.civ.data.game.model.generic.AbstractGameState;
import dd.civ.data.game.model.generic.City;
import dd.civ.data.game.model.generic.Civ;
import dd.civ.data.game.model.generic.Entity;
import dd.civ.data.game.model.generic.MapLocation;
import dd.civ.data.game.model.generic.Unit;
import dd.civ.data.game.model.generic.UnitType;
import dd.civ.data.game.model.generic.impl.AbstractCiv;
import dd.civ.data.game.model.sve.SVECity;
import dd.civ.data.game.model.sve.SVECiv;
import dd.civ.data.game.model.sve.SVEMapEntity;
import dd.civ.data.game.model.sve.SVEUnit;
import dd.civ.data.game.model.sve.impl.AbstractSVEUnit;
import dd.civ.data.game.types.Advance;
import dd.civ.data.game.types.CityImprovement;
import dd.civ.data.game.types.CitySpecialistType;
import dd.civ.data.game.types.CityStatus;
import dd.civ.data.game.types.DifficultyLevel;
import dd.civ.data.game.types.GovernmentType;
import dd.civ.data.game.types.UnitCategory;
import dd.civ.data.game.types.UnitTypeCode;
import dd.civ.data.game.types.VisibilityType;
import dd.civ.data.game.types.WonderType;
import dd.civ.graphics.CivDosRenderer;
import dd.civ.logic.CivUtils;
import dd.jcived.util.ImageUtils;
import dd.jcived.util.Palette;
import dd.jcived.util.ResourceManager;

public class CityProcess {

	//seg007_0043:
	public static void cityProcess(AbstractGameState gs, int arg_cityID, int arg_processMode) {
		int var_B6;
		int var_D8;
		int var_FA;
		int var_C8;
		int var_EA;
		int var_E8;
		int var_X;
		int var_Y;
		int var_neighbourX;
		int var_neighbourY;
		int varOtherDist;
		int varDist;
		int var_cityLoop;
		int var_loopCounter;
		int var_settlerFoodCost;
		int var_16_min_2xDifficulty;
	
		int var_multi1;
	
		// HELPER
		SVECity city = (SVECity) gs.city(arg_cityID);
	
		System.out.println("City ["+arg_cityID+"] exists ? "+city.exists());
		if(!city.exists()) {
			//seg007_5E:
			//seg007_675B:
			return;
		}
	
		//seg007_61:
		int[] var_5x5_cityAreaArray = new int[5*5];
		//CivLogic.dseg_2496_cityViewActiveTab = 0;
		CivLogic.dseg_2496_cityViewActiveTab++;
		CivLogic.dseg_2496_cityViewActiveTab%=3;
		var_B6 = 0;
		var_D8 = 0;
		var_FA = 0;
		var_C8 = 0;
	
		//			var_X = 0;
		//			while(var_X<5) {
		//				var_Y = 0;
		//				while(var_Y<5) {
		//					var_5x5_cityAreaArray[5*var_X+var_Y] = 0;
		//					var_Y++;
		//				}
		//				var_X++;
		//			}
		Arrays.fill(var_5x5_cityAreaArray, 0);
	
		//seg007_CC:
		var_X = city.getLocation().x();
		var_Y = city.getLocation().y();
		CivLogic.currentProcessingCity_dseg_64C2 = arg_cityID;
		varOtherDist = 7;
	
		if(city.has(CityImprovement.PALACE)) {
			//seg007_10E:
			CivLogic.dseg_6AB0_distanceToCapital = 0;
		} else {
			//seg007_114:
			CivLogic.dseg_6AB0_distanceToCapital = 32;
		}
	
		//seg007_117:
		if(city.getShieldStock()>999 ||
				//seg007_12D:
				city.getShieldStock()<0) {
			//seg007_13F:
			city.setShieldStock(0);
		}
	
		// Looping on all cities to detect neighbour squares already used
		// up by another city
		//seg007_14D:
		var_cityLoop = 0;
		while(var_cityLoop<128) {
			if(((SVEMapEntity) gs.city(var_cityLoop)).exists()) {
				City someCity = gs.city(var_cityLoop);
				if(var_cityLoop!=arg_cityID) {
					varDist = CivLogic.distance(var_X,someCity.getLocation().x(),var_Y,someCity.getLocation().y());
					if(varDist<varOtherDist && varDist!=0) {
						varOtherDist = varDist;
					}
					if(city.owner().equals(someCity.owner())) {
						if(someCity.has(CityImprovement.PALACE)) {
							if(varDist<CivLogic.dseg_6AB0_distanceToCapital) {
								CivLogic.dseg_6AB0_distanceToCapital = varDist;
							}
						}
					}
					if(varDist<=5) {
						var_loopCounter = 0;
						while(var_loopCounter<=20) {
							//int w0 = (Byte)someCity.getValue("workers_flag0");
							//int w1 = (Byte)someCity.getValue("workers_flag1");
							//int w2 = (Byte)someCity.getValue("workers_flag2");
							//int workers = (w0&0xFF)|((w1&0xFF)<<8)|((w2&0xFF)<<16);
							//int workerComp = (0x1<<var_loopCounter);
							if(someCity.hasWorkerAt(var_loopCounter) || var_loopCounter==20) {
								var_neighbourX = someCity.getLocation().x() + CivUtils.relCityX[var_loopCounter];
								var_neighbourY = someCity.getLocation().y() + CivUtils.relCityY[var_loopCounter];
								if(Math.abs(var_neighbourX-var_X)<=2 && Math.abs(var_neighbourY-var_Y)<=2) {
									int i = 5*(var_neighbourX-var_X) + var_neighbourY-var_Y;
									var_5x5_cityAreaArray[12+i] = 1;
								}
							}
							var_loopCounter++;
						}
					}
				}
			}
			var_cityLoop++;
		}
	
		//seg007_2F5:
		var_5x5_cityAreaArray[12] = 0;
		CivLogic.cityOwner_dseg_64CA = ((SVECiv)city.owner()).getID();
		Civ cityOwner = gs.civ(CivLogic.cityOwner_dseg_64CA);
		CivLogic.neighbourSquareCount_dseg_64C4 = 21;
		// TODO: Skipping viewport alignment at seg007:0x311
	
	
		if(cityOwner.government().equals(GovernmentType.ANARCHY)
				|| cityOwner.government().equals(GovernmentType.DESPOTISM)) {
			var_settlerFoodCost = 1;
		} else {
			var_settlerFoodCost = 2;
		}
	
		//seg007_338:
		CivLogic.pollutionFactor_dseg_C7A2 = 0;
		if(!city.has(CityImprovement.MASS_TRANSIT)) {
			if(cityOwner.knows(Advance.INDUSTRIALIZATION)) CivLogic.pollutionFactor_dseg_C7A2++;
			if(cityOwner.knows(Advance.AUTOMOBILE)) CivLogic.pollutionFactor_dseg_C7A2++;
			if(cityOwner.knows(Advance.MASS_PRODUCTION)) CivLogic.pollutionFactor_dseg_C7A2++;
			if(cityOwner.knows(Advance.PLASTICS)) CivLogic.pollutionFactor_dseg_C7A2++;
		}
	
		//seg007_3C4:
		var_16_min_2xDifficulty = 10;
		CivLogic.citySpecialistCount_dseg_F7D8 = city.getCitySpecialistCount();
		if(!cityOwner.equals(gs.player())) {
			var_16_min_2xDifficulty = 16 - 2*(Integer)gs.getValue("difficulty");
			if((Short)gs.getValue("year")>=0) {
				if((Short)((Entity) gs.player()).getValue("ranking")==7 // Your ranking impacts the game!
						&& (Integer)gs.getValue("difficulty")>1) {
					var_16_min_2xDifficulty -= 2;
				}
			}
			if(arg_processMode==0) {
				if(!city.has(CityStatus.RIOT)) { // Check if City is in riot
					CivLogic.citySpecialistCount_dseg_F7D8 = 0;
				} else {
					((Entity) city).setPropertyValue("workers_flag0", (byte)0);
					((Entity) city).setPropertyValue("workers_flag1", (byte)0);
					((Entity) city).setPropertyValue("workers_flag2", (byte)0);
					((Entity) city).setPropertyValue("workers_flag3", (byte)0);
				}
			}
		}
	
		//seg007_45D:
	
		// Start drawing the city screen...
		BufferedImage bi = new BufferedImage(320,200,BufferedImage.TYPE_INT_ARGB);
		if(arg_processMode == 1) ImageUtils.animate(bi, city.getName().toUpperCase(), 200);

		//ImageUtils.animate(bi, "DEBUG", 200);
		Graphics2D gfx = bi.createGraphics();
		var_X = city.getLocation().x();
		var_Y = city.getLocation().y();
		if(arg_processMode==1) { // means ax = 1 and dx = 0;
	
			// TODO: deal with call to unkCity_27_28_createFortifiedUnit:
			// it extracts "integrated units" as "real units" for further processing
			// such as displaying them on screen and counting them in unit-related
			// formulas
	
			gfx.setColor(Palette.defaultColors.get(0));
			gfx.fillRect(0, 0, 320, 200);
	
			// city box 1: population
			CivDosRenderer.drawBlueDialogBackground(2, 1, 208, 21, gfx, false);
	
			var_multi1 = 0;
			var_loopCounter = 1;
	
			while(var_loopCounter<=city.actualSize()) {
				var_multi1 += var_loopCounter;
				var_loopCounter++;
			}
	
			String s = city.getName().toUpperCase() + " (Pop:";
			s += CivLogic.populationString(var_multi1);
			s += ")";
	
			CivDosRenderer.drawString(s, gfx, 1, Palette.defaultColors.get(15), 104, 2+FontUtils.getFontHeight(1), 100, 1);
	
	
			// city box 2: city squares
			CivDosRenderer.drawBlueDialogBackground(127, 23, 208, 104, gfx, false);
	
			// Good to know, map viewport gets aligned on (City_X - 5, City_Y - 3)
			ImageUtils.drawMapTerrainSquare(gs, city.getLocation().x(), city.getLocation().y(), bi, 80+5*16,8+3*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
	
			var_loopCounter = 0;
			while(var_loopCounter < CivLogic.neighbourSquareCount_dseg_64C4) {
				var_neighbourX = city.getLocation().x() + CivUtils.relCityX[var_loopCounter];
				var_neighbourY = city.getLocation().y() + CivUtils.relCityY[var_loopCounter];
				if(!city.owner().equals(gs.player()) // Spy or Debug mode: can see all enemy city squares
						|| gs.player().canSee(city.getLocation().neighbour(var_loopCounter, 0))) {
					var_multi1 = CivLogic.getLandOccupation(gs, var_neighbourX, var_neighbourY);
					if(var_multi1==-1
							||var_multi1== ((SVECiv) city.owner()).getID()) {
						ImageUtils.drawMapTerrainSquare(gs, var_neighbourX, var_neighbourY, bi, 80+(5+CivUtils.relCityX[var_loopCounter])*16,8+(3+CivUtils.relCityY[var_loopCounter])*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
						int i = 12 + 5*(CivUtils.relCityX[var_loopCounter]) + CivUtils.relCityY[var_loopCounter];
						if(var_5x5_cityAreaArray[i]!=0) {
							gfx.setColor(Palette.defaultColors.get(12));
							gfx.drawRect(80+(5+CivUtils.relCityX[var_loopCounter])*16,8+(3+CivUtils.relCityY[var_loopCounter])*16, 15, 15);
						}
					} else {
						ImageUtils.drawMapTerrainSquare(gs, var_neighbourX, var_neighbourY, bi, 80+(5+CivUtils.relCityX[var_loopCounter])*16,8+(3+CivUtils.relCityY[var_loopCounter])*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
						//ImageUtils.drawMapSquareItems(gs, cityOwner, var_neighbourX, var_neighbourY, bi, 80+(5+CivUtils.relCityX[var_loopCounter])*16,8+(3+CivUtils.relCityY[var_loopCounter])*16,false, 1);
					}
				}
				var_loopCounter++;
			}
		}
	
		//seg007_6FA:
		if(arg_processMode==0) {
			city.set(CityStatus.IMPROVEMENT_SOLD,false);
			if((Short)((Entity) city).getValue("food_count")<0) {
				var_multi1 = -1;
				var_loopCounter = 0;
				Iterator<SVEUnit> it = ((SVECiv) cityOwner).getUnits(false).iterator();
				//seg007_737:
				while(var_loopCounter<128) {
					SVEUnit unit = it.next();
					if(unit.typeID()==0 && (Short)((Entity) unit).getValue("home_city_id")==arg_cityID) {
						var_multi1 = var_loopCounter;
						break;
					}
					var_loopCounter++;
				}
				//seg007_78B:
				if(cityOwner.equals(gs.player())) {
					// TODO: pop-up message:
					// String s = "Food storage exhausted\nin "+city.getName();
					// if(var_multiusage_bulbs_pop_==-1) {
					//   s+= "!\nFamine feared.\n"
					// } else {
					//   s+= "!\nSettlers lost.\n"
					// }
					// Show newspaper headlines with s, play music, etc.
				}
				//seg007_7F5:
				if(var_multi1==-1) {
					((Entity) city).setPropertyValue("actual_size",(byte)(city.actualSize()-1));
					if(city.actualSize()==0) {
						// TODO
						//seg007_823:
						// Deal with city destroyed, and possibly Civ destroyed...
					}
				} else {
					//seg007_7FF:
					// TODO: remove lost settlers
				}
	
				//seg007_85C:
				// TODO: refresh map square 
				ImageUtils.drawMapTerrainSquare(gs, var_X, var_Y, bi, 80+(5+0)*16,8+(3+0)*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
				ImageUtils.drawMapSquareItems(gs, (AbstractCiv) cityOwner, var_X, var_Y, bi,bi,bi, 80+(5+0)*16,8+(3+0)*16,false, 1);
	
				((Entity) city).setPropertyValue("food_count",(short)0);
			}
			//seg007_87A:
			if((city.actualSize()+1)*var_16_min_2xDifficulty<=(Short)((Entity) city).getValue("food_count")) {
				((Entity) city).setPropertyValue("actual_size",(byte)(city.actualSize()+1));
				if(city.has(CityImprovement.GRANARY)) {
					((Entity) city).setPropertyValue("food_count", (short)(5*city.actualSize()+5));
				} else {
					((Entity) city).setPropertyValue("food_count", (short)0);
				}
				if(city.actualSize()>10 && !city.has(CityImprovement.AQUEDUCT)
						/* && DebugSwitch 0x100 is set to 1 */) {
					((Entity) city).setPropertyValue("actual_size",(byte)(city.actualSize()-1));
					if(cityOwner.equals((gs.player()))) {
						// TODO: String popup:
						// citName + " requires an AQUEDUCT\nfor further growth."
					}
				}
				ImageUtils.drawMapTerrainSquare(gs, var_X, var_Y, bi, 80+(5+0)*16,8+(3+0)*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
				ImageUtils.drawMapSquareItems(gs, cityOwner, var_X, var_Y, bi, bi, bi, 80+(5+0)*16,8+(3+0)*16,false, 1);
			}
		}
	
		//seg007_95E:
		int var_citySize_plus_1 = city.actualSize() + 1;
		CivLogic.dseg_64C8_away_unit_counter = 0;
		CivLogic.dseg_EDD8_unk27_28_counter = 0;
		CivLogic.dseg_E216_unitShieldMaintenanceCost = 0;
		CivLogic.dseg_F2E6_settler_counter = 0;
	
		var_loopCounter = 0;
		//seg007_98B:
		while(var_loopCounter<2) {
			if(((Short)((Entity) city).getValue("unknown_cb2"+(7+var_loopCounter))&0xFF)!=0xFF) {
				CivLogic.dseg_EDD8_unk27_28_counter++;
			}
			var_loopCounter++;
		}
		//seg007_9B2:
		if(city.actualSize()<CivLogic.dseg_EDD8_unk27_28_counter) {
			CivLogic.dseg_E216_unitShieldMaintenanceCost = CivLogic.dseg_EDD8_unk27_28_counter - city.actualSize();
		}
	
		//seg007_9DF:
		var_loopCounter = 0;
		Iterator<SVEUnit> it = ((SVECiv) cityOwner).getUnits(false).iterator();
		while(var_loopCounter<128) {
			SVEUnit _SVEUnit = it.next();
			if((_SVEUnit.typeID()&0xFF)!=0xFF && (Short)((Entity) _SVEUnit).getValue("home_city_id")==arg_cityID) {
				if(_SVEUnit.typeID()<26) {
					if(city.actualSize()<CivLogic.dseg_EDD8_unk27_28_counter
							|| !(cityOwner.government().equals(GovernmentType.ANARCHY)||cityOwner.government().equals(GovernmentType.DESPOTISM))) {
						/*if DebugSwitches 0x10 is set to 1 */
						CivLogic.dseg_E216_unitShieldMaintenanceCost++;
					}
					if(_SVEUnit.typeID()!=0) {
						if((Short)((Entity) _SVEUnit.getType()).getValue("terrain_cat")==1
								|| _SVEUnit.getLocation().x()!=city.getLocation().x()
								|| _SVEUnit.getLocation().y()!=city.getLocation().y()) {
							CivLogic.dseg_64C8_away_unit_counter++;
						}
					}
	
				}
				if(_SVEUnit.typeID()==0) {
					//seg007_B38:
					CivLogic.dseg_F2E6_settler_counter++;
				}
			}
			//seg007_9E8:
			var_loopCounter++;
		}
	
		//seg007_B3F:
		int[] var_occupiedSquares = new int[CivLogic.neighbourSquareCount_dseg_64C4];
		CivLogic.dseg_7068_playerTrespassingAIcityFlag = 0;
		var_loopCounter = 0;
		while(var_loopCounter<CivLogic.neighbourSquareCount_dseg_64C4) {
			var_occupiedSquares[var_loopCounter] = 0;
	
			var_neighbourX = city.getLocation().x() + CivUtils.relCityX[var_loopCounter];
			var_neighbourY = city.getLocation().y() + CivUtils.relCityY[var_loopCounter];
	
			if(((SVECiv) cityOwner).getID()!=0) {
				if(!cityOwner.canSee(city.getLocation().neighbour(var_loopCounter, 0))) {
					var_occupiedSquares[var_loopCounter] = 1;
				}
			}
	
			int i = CivUtils.relCityX[var_loopCounter]*5 + CivUtils.relCityY[var_loopCounter];
	
			Civ occup = CivLogic.whatCivOccupies(gs, var_neighbourX, var_neighbourY);
			//int var_multiusage_bulbs_pop_ = gs.getMap().getRawLayerValue(8, var_neighbourX, var_neighbourY);
	
			//var_multiusage_bulbs_pop_ &= 0xFF;
			if(occup!=null) {
				if(!cityOwner.equals(occup)) {
					//seg007_c02
					var_occupiedSquares[var_loopCounter] = 1;
					var_5x5_cityAreaArray[i+12] = 1;
					if(occup.equals(((SVECiv) gs.player()).getID())) {
						CivLogic.dseg_7068_playerTrespassingAIcityFlag = 1;
					}
				}
			}
	
			if(var_5x5_cityAreaArray[i+12]!=0) {
				var_occupiedSquares[var_loopCounter] = 1;
			}
	
			if(var_occupiedSquares[var_loopCounter]!=0) {
				//						// remove worker, if any
				//						int w0 = (Byte)city.getValue("workers_flag0");
				//						int w1 = (Byte)city.getValue("workers_flag1");
				//						int w2 = (Byte)city.getValue("workers_flag2");
				//						//int w3 = (Byte)city.getValue("workers_flag3");
				//						int workers = (w0&0xFF)|((w1&0xFF)<<8)|((w2&0xFF)<<16)/*|((w3&0xFF)<<24)*/;
				//						int workerComp = ~(0x1<<var_loopCounter);
				//						workers &= workerComp;
				//						w0 = workers&0xFF;
				//						w1 = (workers>>8)&0xFF;
				//						w2 = (workers>>16)&0xFF;
				//						//w3 = (workers>>24)&0xFF;
				//						city.setValue("workers_flag0", (byte)w0);
				//						city.setValue("workers_flag1", (byte)w1);
				//						city.setValue("workers_flag2", (byte)w2);
				//						//city.setValue("workers_flag3", (byte)w3);
				city.setWorkerAt(var_loopCounter, false);
			}
			var_loopCounter++;
		}
	
		//seg007_C9A:
		var_loopCounter = 0;
		CivLogic.cityFoodProd_dseg_705A = 0;
		CivLogic.cityShieldProd_dseg_705C = 0;
		CivLogic.cityTradeProd_dseg_705E = 0;
		CivLogic.cityLuxuryProd_dseg_7060 = 0;
		//			while(var_loopCounter<4) {
		//				dseg_705A_resourceCounters[var_loopCounter] = 0;
		//				var_loopCounter++;
		//			}
	
		//seg007_CC0:
		var_E8 = 0;
		var_EA = 0;
	
		boolean _skip0 = false;
		if(cityOwner.equals(gs.player())
				|| ((arg_cityID + (Integer)gs.getValue("game_turn")) & 0b11) != 0 // definitely some kind of randomization
				|| (city.has(CityStatus.RIOT))  // city is rioting
				|| (CivLogic.dseg_7068_playerTrespassingAIcityFlag!=0)) {
			//seg007_D07:
			var_loopCounter = 0;
			while(var_loopCounter<CivLogic.neighbourSquareCount_dseg_64C4) {
				if(city.hasWorkerAt(var_loopCounter)) {
					var_citySize_plus_1--;
				}
				var_loopCounter++;
			}
	
			if(var_citySize_plus_1>=0) {
				var_citySize_plus_1 = city.actualSize() + 1;
				var_loopCounter = 0;
				while(var_loopCounter<CivLogic.neighbourSquareCount_dseg_64C4) {
					if(var_citySize_plus_1 != 0) {
						//System.out.println(" city has worker at id="+var_loopCounter+" : "+city.hasWorkerAt(var_loopCounter));
						if(city.hasWorkerAt(var_loopCounter)) {
							var_occupiedSquares[var_loopCounter] = 1;
	
							CivLogic.drawResourcesOnCityMapSquare(gs, bi, arg_cityID, var_loopCounter, arg_processMode);
	
							var_citySize_plus_1 --;
	
							var_EA |= (1<<var_loopCounter)&0x0000FFFF;
							var_E8 |= ((1<<var_loopCounter)&0xFFFF0000)>>16;
						}
					}
					var_loopCounter++;
				}
	
			} else {
				if(arg_processMode==0) {
					if(cityOwner.equals(gs.player())) {
						//TODO: popup message:
						// "Population decrease\nin "
						// city name
						// ".\n"
						CivLogic.dseg_2F4E = 4;
						CivLogic.dseg_C108_openCityScreenFlag = 1;
					}
				}
				var_citySize_plus_1 = city.actualSize() + 1;
			}
	
			if(CivLogic.citySpecialistCount_dseg_F7D8 == var_citySize_plus_1
					|| var_citySize_plus_1==0) {
				_skip0 = true;
			}
		}
	
		int var_bestNeighbourSquareID = 0; // ? no initialization...
		int var_foodProd;
		if(!_skip0) {
			//seg007_E7D:
			if(var_citySize_plus_1>0) {
				if(var_occupiedSquares[20] == 0) {
					var_occupiedSquares[20] = 1;
					CivLogic.drawResourcesOnCityMapSquare(gs, bi, arg_cityID, 20, arg_processMode);
					var_EA |= 0;
					var_E8 |= 0x10;
					var_citySize_plus_1--;
				}
			}
			//int AX = var_citySize_plus_1 + (var_citySize_plus_1<0?1:0);
			//AX/=2;
			//AX+=dseg_705A[0];
			//int CX = dseg_705A[0]+var_citySize_plus_1 + (var_citySize_plus_1<0?1:0)/2;
			//int AX = city.getActualSize()*2;
			//int DX = city.getActualSize()*2;
			//int AX = var_govtBonus_1_or_2;
			//int BX = city.getActualSize()*2+dseg_F2E6_settler_counter*var_govtBonus_1_or_2;
			//AX *= dseg_F2E6_settler_counter;
			//BX += dseg_F2E6_settler_counter*var_govtBonus_1_or_2;
	
			int var_bestSquareFitness; // ? no initialization...
			int var_bestFoodProd; // ? no initialization...
			int var_squareFitness;
			// Seems to be "worker auto-assign" loop
			//seg007_EB5:
			while( (
					(city.actualSize()*2+CivLogic.dseg_F2E6_settler_counter*var_settlerFoodCost) // Food consumption
					>
					(CivLogic.cityFoodProd_dseg_705A+(var_citySize_plus_1 + (var_citySize_plus_1<0?1:0))/2) // Food production ?
					|| city.actualSize()<3
					)
					&& 
					(var_citySize_plus_1>CivLogic.citySpecialistCount_dseg_F7D8)
					&&
					(var_bestNeighbourSquareID!=-1)) {
	
				var_bestNeighbourSquareID = -1;
				var_bestSquareFitness = 0;
				var_bestFoodProd = 0;
				var_loopCounter = 0;
	
				// Find next best, available city square
				while(var_loopCounter<CivLogic.neighbourSquareCount_dseg_64C4) {
					if(var_occupiedSquares[var_loopCounter] == 0) { // not occupied
	
						var_neighbourX = city.getLocation().x() + CivUtils.relCityX[var_loopCounter];
						var_neighbourY = city.getLocation().y() + CivUtils.relCityY[var_loopCounter];
						var_foodProd = CivLogic.computeMapSquareResources(gs, var_neighbourX, var_neighbourY, 0);
						var_squareFitness = CivLogic.computeMapSquareResources(gs, var_neighbourX, var_neighbourY, 1);
						if(var_citySize_plus_1 != 1
								|| CivLogic.cityShieldProd_dseg_705C != 0 // shields
								|| var_squareFitness != 0) {
							var_squareFitness += CivLogic.computeMapSquareResources(gs, var_neighbourX, var_neighbourY, 2);
	
							if(var_foodProd>var_bestFoodProd
									|| (var_foodProd==var_bestFoodProd && var_squareFitness>var_bestSquareFitness)) {
								var_bestFoodProd = var_foodProd;
								var_bestSquareFitness = var_squareFitness;
								var_bestNeighbourSquareID = var_loopCounter;
							}
						}
					}
					var_loopCounter++;
				}
	
				if(var_bestNeighbourSquareID!=-1) {
					var_occupiedSquares[var_bestNeighbourSquareID] = 1; // occupy square w/ specialist
					CivLogic.drawResourcesOnCityMapSquare(gs, bi, arg_cityID, var_bestNeighbourSquareID, arg_processMode);
					var_EA |= (1<<var_bestNeighbourSquareID)&0x0000FFFF;
					var_E8 |= ((1<<var_bestNeighbourSquareID)&0xFFFF0000)>>16;
					//city.setWorkerAt(var_bestNeighbourSquareID, true);
					var_citySize_plus_1--;
				}
			}
	
			//seg007_1074:
			if(!cityOwner.equals(gs.player())) {
				if(city.has(CityStatus.RIOT)) { // city is rioting?
					//seg007_1092
					CivLogic.citySpecialistCount_dseg_F7D8++;
				}
			}
	
			//seg007_1096:
			while(var_citySize_plus_1>CivLogic.citySpecialistCount_dseg_F7D8) {
				var_bestFoodProd = 0;
				var_bestSquareFitness = 0;
				var_bestNeighbourSquareID = -1;
				int var_E0 = 0;
				var_loopCounter = 0;
				while(var_loopCounter<CivLogic.neighbourSquareCount_dseg_64C4) {
					if(var_occupiedSquares[var_loopCounter]==0) {
						//seg007_10DE
						var_neighbourX = city.getLocation().x() + CivUtils.relCityX[var_loopCounter];
						var_neighbourY = city.getLocation().y() + CivUtils.relCityY[var_loopCounter];
	
						int foodProduction = CivLogic.cityFoodProd_dseg_705A;
						int foodConsumption = city.actualSize()*2 + var_settlerFoodCost * CivLogic.dseg_F2E6_settler_counter;
						int foodSurplus = CivLogic.rangeBound(foodProduction - foodConsumption,1,99);
	
						//int SI = 16/foodSurplus;
	
						int var_foodProd_2 = CivLogic.computeMapSquareResources(gs, var_neighbourX,var_neighbourY,0);
						var_squareFitness = (16/foodSurplus) * var_foodProd_2;
	
						int var_shieldProd_2 = CivLogic.computeMapSquareResources(gs, var_neighbourX,var_neighbourY,1);
	
						int shieldProduction = CivLogic.cityShieldProd_dseg_705C;
						int shieldConsumption = CivLogic.dseg_E216_unitShieldMaintenanceCost;
						int shieldSurplus = CivLogic.rangeBound(shieldProduction - shieldConsumption,1,99);
	
						//int CX = rangeBound(dseg_705A_resourceCounters[1]-dseg_E216_unitShieldMaintenanceCost,1,99);
						//int AX = (city.getActualSize()*3/shieldSurplus)*var_shieldProd_2;
						var_squareFitness += (city.actualSize()*3/shieldSurplus)*var_shieldProd_2;
	
						int var_tradeProd = CivLogic.computeMapSquareResources(gs, var_neighbourX,var_neighbourY,2);
	
						int AX = CivLogic.rangeBound(CivLogic.cityTradeProd_dseg_705E,1,99);
						var_squareFitness += (city.actualSize()*2/AX)*var_tradeProd;
	
						if(var_squareFitness>var_bestSquareFitness) {
							var_bestSquareFitness = var_squareFitness;
							var_bestNeighbourSquareID = var_loopCounter;
							var_E0 = 2*(var_foodProd_2+var_shieldProd_2)+var_tradeProd;
						}
					}
					var_loopCounter++;
				}
				if(var_bestNeighbourSquareID!=-1) {
					var_occupiedSquares[var_bestNeighbourSquareID] = 1; // occupy square w/ worker
					CivLogic.drawResourcesOnCityMapSquare(gs, bi, arg_cityID, var_bestNeighbourSquareID, arg_processMode);
					var_EA |= (1<<var_bestNeighbourSquareID)&0x0000FFFF;
					var_E8 |= ((1<<var_bestNeighbourSquareID)&0xFFFF0000)>>16;
					var_citySize_plus_1--;
				} else {
					break;
				}
			}
	
		}
		//seg007_127E:
		var_E8 |= ((var_citySize_plus_1<<10)&0b1111110000000000);
		((Entity) city).setPropertyValue("workers_flag0", (byte)(var_EA&0xFF));
		((Entity) city).setPropertyValue("workers_flag1", (byte)((var_EA>>8)&0xFF));
		((Entity) city).setPropertyValue("workers_flag2", (byte)(var_E8&0xFF));
		((Entity) city).setPropertyValue("workers_flag3", (byte)((var_E8>>8)&0xFF));
	
	
		// =====================================
		// Major loop start - seg007_12AE
		// =====================================
		//seg007_12AE:
		var_X = city.getLocation().x();
		var_Y = city.getLocation().y();
		int var_specialistCount = var_citySize_plus_1;
		var_loopCounter = 0;
	
		//seg007_12AE:
		while(var_loopCounter<var_specialistCount) {
			if(city.getCitySpecialistType(var_loopCounter).equals(CitySpecialistType.NONE)) {
				city.setCitySpecialistType(var_loopCounter, CitySpecialistType.ENTERTAINER);
			}
			var_loopCounter++;
		}
		var_loopCounter = var_specialistCount;
		//seg007_1326:
		while(var_loopCounter<8) {
			city.setCitySpecialistType(var_loopCounter, CitySpecialistType.NONE);
			var_loopCounter++;
		}
	
		//seg007_1343:
		int var_prodMultiplier2 = 0;
		int var_prodMultiplier1 = 0;
		CivLogic.dseg_6C18_cityPowerType = 1;
		if(city.has(CityImprovement.FACTORY)) { var_prodMultiplier1 = 2; }
		if(city.has(CityImprovement.MFG_PLANT)) { var_prodMultiplier1 = 4; }
		if(city.has(CityImprovement.POWER_PLANT)) { var_prodMultiplier2 = 2; }
		if(city.has(CityImprovement.HYDRO_PLANT)) { CivLogic.dseg_6C18_cityPowerType = 2; var_prodMultiplier2 = 2; }
		if(city.has(CityImprovement.NUCLEAR_PLANT)) { CivLogic.dseg_6C18_cityPowerType = 2; var_prodMultiplier2 = 2; }
		if(CivLogic.isWonderApplicable(cityOwner,WonderType.HOOVER_DAM)) {
			int hx = gs.wonder(WonderType.HOOVER_DAM.getId()-1).getHostCity().getLocation().x();
			int hy = gs.wonder(WonderType.HOOVER_DAM.getId()-1).getHostCity().getLocation().y();
			int hcid = gs.landMassID(new MapLocation(hx,hy));
			int cx = city.getLocation().x();
			int cy = city.getLocation().y();
			int ccid = gs.landMassID(new MapLocation(cx,cy));
			if(hcid==ccid) {
				CivLogic.dseg_6C18_cityPowerType = 2; var_prodMultiplier2 = 2;
			}
		}
		if(city.has(CityImprovement.RECYCLING_CENTER)) { CivLogic.dseg_6C18_cityPowerType = 3; }
	
		//seg007_1464:
		var_prodMultiplier2 = CivLogic.rangeBound(var_prodMultiplier2, 0, var_prodMultiplier1);
		int var_cityShieldProd = CivLogic.cityShieldProd_dseg_705C;
		int var_cityTradeProd = CivLogic.cityTradeProd_dseg_705E;
		CivLogic.cityShieldProd_dseg_705C += (var_prodMultiplier1*CivLogic.cityShieldProd_dseg_705C)/4 + (var_prodMultiplier2*CivLogic.cityShieldProd_dseg_705C)/4;
		if(arg_processMode==0) {
			//seg007_14CA:
			int food = city.getFoodStock()+CivLogic.cityFoodProd_dseg_705A-var_settlerFoodCost*CivLogic.dseg_F2E6_settler_counter - city.actualSize()*2; 
			city.setFoodStock(food);
			var_multi1 = CivLogic.cityShieldProd_dseg_705C - CivLogic.dseg_E216_unitShieldMaintenanceCost;
			if(city.has(CityStatus.RIOT)) {
				//seg007_150E:
				var_multi1 = 0;
			}
			//seg007_1514:
			city.setShieldStock(city.getShieldStock()+var_multi1);
			boolean goto_seg007_22D9 = false; // strange control impossible to port otherwise; maybe a goto in original code
			if(city.getCurrentProductionID()>=0) {
				//seg007_1536:
				int cost = gs.unitType(city.getCurrentProductionID()).cost()*var_16_min_2xDifficulty;
				if(cost<=city.getShieldStock() &&
						//seg007_1556:
						( city.getCurrentProductionID() != 0
						||
						//seg007_1568:
						city.actualSize() != 1
						||
						//seg007_157A:
						gs.getDifficultyLevel() != DifficultyLevel.CHIEFTAIN)) {
					//seg007_1587:
					Unit newUnit = null;
					city.setShieldStock(city.getShieldStock()-cost);
					if(cityOwner.equals(gs.player())
							//seg007_15B4:
							|| city.getCurrentProductionID() != 26) {
						//seg007_15C6:
						newUnit = CivLogic.createUnit(cityOwner,gs.unitType(city.getCurrentProductionID()),city.getLocation().x(),city.getLocation().y());
					}
					//seg007_15F2:
					if(gs.unitFirstTimeBuilt((UnitType) city.getCurrentProduction())) {
						//seg007_1612:
						//Skipped: write Replay Entry for unit first time built
						// with arguments: 6, 2 cityOwner, city.currentProdID
						gs.setUnitFirstTimeBuilt((UnitType) city.getCurrentProduction(), true);
					}
					//seg007_1650:
					if(city.has(CityImprovement.BARRACKS)
							&&
							//seg007_1662:
							newUnit != null) {
						//seg007_166C:
						newUnit.setVeteran(true);
					}
					//seg007_1683:
					if(newUnit != null) {
						//seg007_168D:
						if(city.getCurrentProductionID() == 0) {
							//seg007_169F:
							if(city.actualSize() > 1
									||
									//seg007_16B1:
									cityOwner.cities().size() > 1) {
								//seg007_16C1:
								city.actualSize(city.actualSize() - 1);
								if(city.actualSize() == 0) {
									//seg007_16D2:
									//TODO destroy city, and check whether Civ is destroyed too...
								}
							}
						}
					}
					//seg007_1737:
					if(!cityOwner.equals(gs.player())) {
						//seg007_1743:
						if(city.getCurrentProductionID() == 27) { // AI just produced a Caravan
							//seg007_1755:
							var_bestNeighbourSquareID = -1;
							int var_C6 = -1;
							var_cityLoop = 0;
							//seg007_176B:
							while(var_cityLoop < 0x80) {
								//TODO: lookup the other, non-player City that has the best BaseTrade score
								var_cityLoop++;
							}
							//seg007_17E4:
							// TODO ...and establish a trade route with them
	
						}
						//seg007_17F8:
						if(city.getCurrentProductionID() == 26) { // AI just produced a Diplomat
							//TODO:
							// find a player city, that didn't have tech stolen this turn,
							// and which has a city or unit on the same continent (diplomat not on ocean, of course)
	
							//seg007_1991:
							//if city is more than 3 squares away, or player at peace with AI civ: reimburse the diplomat (cheating AI!)
							//otherwise, create a Diplomat on the position of nearest city/unit and assign its GoTo to player city
						}
						//seg007_1A59:
						if(city.getCurrentProductionID() == 25) { // AI just produced a Nuclear
							//TODO:
							//if the AI civ has totally 1 Active Nuclear (just created), 
							// then prepare to meet the Player (nextContact set to -1)
						}
	
						//seg007_1A8A:
						//call: selectCityProduction(city)
	
					} else {
						//seg007_1A98:
						city.setShieldStock(0);
						if(newUnit != null) {
							//seg007_1AB0:
							if(city.getCurrentProductionID() == 0
									||
									//seg007_1AC2:
									city.getCurrentProductionID() >= 26) {
								//TODO: popup a dialog to tell the player a Settler, Diplomat or Caravan was just built
								CivLogic.dseg_C108_openCityScreenFlag = 1;
							}
	
						}
					}
					//seg007_1B42:
				} else {
					goto_seg007_22D9 = true;
				}
			} else { //prod ID < 0, i.e. a building or wonder was just built
				//seg007_1B45:
				if(city.getCurrentProduction().getCostBase()*var_16_min_2xDifficulty <= city.getShieldStock()) {
					var_multi1 = -city.getCurrentProductionID();
					if(var_multi1 > 24) { // just built a wonder
						//seg007_1B88:
						if(gs.wonder(var_multi1 - 24).getHostCity() == null) {
							//seg007_1B98:
							//gs.wonder(var_multi1 - 24).setHostCity(city);
							//write replay entry for wonder built
						} else {
							var_multi1 = -1;
						}
					} else {
						//seg007_1BCD:
						if(city.has((CityImprovement)city.getCurrentProduction())) {
							var_multi1 = -1;
						}
					}
					//seg007_1BFF:
					if(var_multi1 != -1) {
						if(var_multi1 == 41) { // Project Manhattan
							//seg007_1C13:
							//Newspaper headlines: "Sensors report a NUCLEAR WEAPONS test near"...
						}
						//seg007_1C62:
						if(var_multi1 < 22) {
							//seg007_1C6C:
							city.add((CityImprovement)city.getCurrentProduction());
						}
						//seg007_1C91:
						city.setShieldStock(city.getShieldStock() - var_16_min_2xDifficulty*city.getCurrentProduction().getCostBase());
						if(!cityOwner.equals(gs.player())
								//seg007_1CC4:
								|| var_multi1>24) {
							//seg007_1CCE:
							if(cityOwner.equals(gs.player())) {
								//seg007_1CE0;
								CivLogic.dseg_C108_openCityScreenFlag = 1; // open screen after building built
							} else {
								//seg007_1CE9:
								for(int i=0;i<8;i++) {
									gs.setVisibility(gs.civ(0), city.getLocation().x(), city.getLocation().y(), 1, 1, VisibilityType.VISIBLE);
								}
							}
							//seg007_1D04:
							//Skipped: prepare newspaperheadlines to inform player of construction
							if(gs.player().equals(cityOwner)) {
								//seg007_1D57:
								if(((Short)gs.getValue("game_settings") & 0x8) != 0) {
									//seg07_1D61:
								}
							}
							//seg007_1DBB:
						}
						//seg007_1DD7:
					}
				}
			}
			if(!goto_seg007_22D9) {
				//seg007_21E1:
				if(cityOwner.equals(gs.player())) {
					//seg007_21ED:
					if(city.has(CityStatus.AUTO_BUILD)) {
						//seg007_21FF:
						if(city.getShieldStock() == 0
								||
								//seg007_2211:
								(city.getCurrentProductionID()<0
										//seg007_2223:
										&& var_multi1 == -1)) {
							//seg007_222D:
	
							//TODO: auto-select city production for player
							//store auto-prod result in var_multi1
							//if auto-production could not find a suitable prod, result is 99
							if(var_multi1 != 99) {
								//seg007_2253:
								CivLogic.dseg_C108_openCityScreenFlag = 0;
								if(city.getCurrentProductionID() >= 0) {
									//seg007_226B:
									//decrease PerCivUnitsInProduction...
								}
								//seg007_2289:
								city.setCurrentProductionID(var_multi1);
								if(city.getCurrentProductionID() >= 0) {
									//seg007_22AB:
									//increase PerCivUnitsInProduction...
								}
								//seg007_22C9:
							} else {
								//seg007_22CC:
								city.set(CityStatus.AUTO_BUILD,  false);
							}
						}
					}
				}
			}
	
			//seg007_22D9:
			if(!city.owner().equals(gs.player())) {
				//seg007_22EB:
				if(city.has(CityStatus.AUTO_BUILD)) {
					//seg007_22FD:
					//TODO!! select city production
				}
				//seg007_2308:
				var_foodProd = 0;
				int var_policy = (short)(Short)gs.getValue("civ"+cityOwner.getID()+".geostrategy"+gs.landMassID(var_X, var_Y)+".policy");
				if(var_policy == 1
						||
						//seg007_233C:
						var_policy == 2
						||
						//seg007_2346:
						var_policy == 5
						) {
					//seg007_2350:
					if(var_multi1 == 0) {
						//seg007_235A:
						if(city.getCurrentProductionID() >= 0) {
							//seg007_236C:
							if(gs.unitType(city.getCurrentProductionID()).role().id() == var_policy) {
								//seg007_2389:
								var_foodProd = cityOwner.gold()>>6;
							}
						}
					}
				}
				//seg007_23A5:
				if(gs.player().hasLaunchedSpaceShip()) {
					//seg007)23BA:
					if(city.getCurrentProductionID()>=22) {
						//seg007)23D1:
						if(city.getCurrentProductionID()<=24) {
							//seg007_23E8:
							var_foodProd = cityOwner.gold()>>7;
						}
					}
				}
				//seg007_2404:
				if(city.has(CityStatus.RIOT)) {
					//seg007_2416:
					if(city.getCurrentProductionID() < 0) {
						//seg007_2428:
						if(city.getShieldStock() != 0) {
							//seg007_243A:
							var_foodProd = CivLogic.rangeBound(city.getCurrentProduction().getCostBase()*var_16_min_2xDifficulty - city.getShieldStock(), 0, cityOwner.gold()>>3);
						}
					}
				}
				//seg007_2485:
				if(CivLogic.whatCivOccupies(gs, var_X, var_Y).getID() == 0
						||
						//seg007_249B:
						city.has(CityStatus.AUTO_BUILD)) {
					//seg007_24AD:
					if(city.getCurrentProductionID() >= 0) {
						//seg007_24BF:
						if(city.getShieldStock() != 0) {
							//seg007_24D1:
							var_foodProd = CivLogic.rangeBound(city.getCurrentProduction().getCostBase()*var_16_min_2xDifficulty - city.getShieldStock(), 0, cityOwner.gold() / 3);
						}
					}
				}
				//seg007_250E:
				if(city.getCurrentProduction() == null) {
					//seg007_2520:
					if(city.getShieldStock()!=0) {
						//seg007_2532:
						var_foodProd = CivLogic.rangeBound(city.getCurrentProduction().getCostBase()*var_16_min_2xDifficulty - city.getShieldStock(), 0, cityOwner.gold() / 3);
					}
				}
				//seg007_256F:
				if(city.has(CityStatus.RIOT) // cmp CityData.Status[bx], 19h
						&& !city.has(CityStatus.COASTAL)
						&& !city.has(CityStatus.RAPTURE_1)
						&&  city.has(CityStatus.HYDRO_AVAILABLE)
						&&  city.has(CityStatus.AUTO_BUILD)
						&& !city.has(CityStatus.TECH_STOLEN)
						&& !city.has(CityStatus.RAPTURE_2)
						&& !city.has(CityStatus.IMPROVEMENT_SOLD)
						) {
					//seg007_2581:
					//if(cityOwner has no nukes) {
					//seg007_2594:
					if(city.getShieldStock() != 0) {
						//seg007_25A6:
						var_foodProd = CivLogic.rangeBound(city.getCurrentProduction().getCostBase()*var_16_min_2xDifficulty - city.getShieldStock(), 0, cityOwner.gold() >> 2);
					}
					//}
				}
	
				//seg007_25EB:
				if(cityOwner.gold() > 2000) {
					//seg007_25FC:
					var_foodProd += (cityOwner.gold() >> 9); 
				}
				//seg007_2618:
				city.setShieldStock(city.getShieldStock() + var_foodProd);
				cityOwner.setGold(cityOwner.gold() - 2*var_foodProd);
				city.set(CityStatus.AUTO_BUILD, false);
			}
		}
		
		//seg007_2645:
		if(arg_processMode == 1) {
			//seg007_2651:
			 
			//city box 3: resources?
			CivDosRenderer.drawBlueDialogBackground(2, 67, 124, 104, gfx, false);
	
			//city box 4: happiness+map+?
			CivDosRenderer.drawBlueDialogBackground(95, 106, 227, 197, gfx, false);
	
			//tab INFO
			CivDosRenderer.drawPushButton(95, 106, 128, 114, bi, "INFO", Palette.defaultColors.get(9));
			//tab HAPPY
			CivDosRenderer.drawPushButton(129, 106, 160, 114, bi, "HAPPY", Palette.defaultColors.get(9));
			//tab MAP
			CivDosRenderer.drawPushButton(161, 106, 193, 114, bi, "MAP", Palette.defaultColors.get(9));
			//tab VIEW
			CivDosRenderer.drawPushButton(194, 106, 226, 114, bi, "VIEW", Palette.defaultColors.get(9));
			
			//CivLogic.dseg_2496_cityViewActiveTab = 1;
			CivDosRenderer.replaceColor(bi, 96+33*CivLogic.dseg_2496_cityViewActiveTab, 107, 32, 7, 9, 15);
			
			if(CivLogic.dseg_2496_cityViewActiveTab == 2) {
				CivDosRenderer.drawCityMiniMap(bi, gs);
			}
		}
		
		//seg007_2750:
		var_X = city.getLocation().x();
		var_Y = city.getLocation().y();
		
		CivLogic.dseg_E200_corruption = 0;
		if(cityOwner.government().equals(GovernmentType.COMMUNISM)) {
			//seg007_278A:
			CivLogic.dseg_6AB0_distanceToCapital = 10;
		}
		
		//seg007_2790:
		CivLogic.dseg_E200_corruption = (CivLogic.cityTradeProd_dseg_705E * CivLogic.dseg_6AB0_distanceToCapital * 3) / (20 * ((SVECiv)cityOwner).governmentID() + 80);
	
		if(city.has(CityImprovement.COURTHOUSE)
				||
				//seg007_27C6:
				city.has(CityImprovement.PALACE)) {
			//seg007_27D8:
			CivLogic.dseg_E200_corruption = CivLogic.dseg_E200_corruption / 2;
		}
	
		//seg007_27E4:
		if(cityOwner.government().equals(GovernmentType.DEMOCRACY)) {
			//seg007_27F4:
			CivLogic.dseg_E200_corruption = 0;
		}
	
		//seg007_27FA:
		city.setBaseTrade(CivLogic.cityTradeProd_dseg_705E - CivLogic.dseg_E200_corruption);
	
		//seg007_281A:
		for(City tradeCity : city.getTradeCities()) {
			//seg007_2824:
			SVECity sveCity = (SVECity)tradeCity;
			if(sveCity != null && sveCity.exists()) {
				//seg007_2843:
				int cx = 3;
				if(sveCity.owner().equals(cityOwner)) {
					//seg007_2882:
					cx = 4;
				} else {
					//seg007_2859:
				}
				CivLogic.cityTradeProd_dseg_705E += (sveCity.getBaseTrade() + CivLogic.cityTradeProd_dseg_705E + 4) >> cx; 
			}
		}
		//seg007_28AB:
		
//		int ax = 20*cityOwner.government().getID();
//		int cx = ax;
//		cx = cx+80;
//		int bx = 3;
//		ax = CivLogic.cityTradeProd_dseg_705E;
//		ax = ax*CivLogic.dseg_6AB0_distanceToCapital;
//		ax = ax*bx;
//		ax = ax/cx;
//		CivLogic.dseg_E200_corruption = ax;

		CivLogic.dseg_E200_corruption = CivLogic.cityTradeProd_dseg_705E*CivLogic.dseg_6AB0_distanceToCapital*3/(20*cityOwner.government().getID()+80);

		if(cityOwner.government().equals(GovernmentType.DEMOCRACY)) {
			//seg007_28DF:
			CivLogic.dseg_E200_corruption = 0;
		}

		//seg007_28E5:
		if(city.has(CityImprovement.COURTHOUSE)) {
			//seg007_28F7:
			CivLogic.dseg_E200_corruption = CivLogic.dseg_E200_corruption/2;
		}
		
		//seg007_2903:
		
//		int cx = CivLogic.cityTradeProd_dseg_705E;
//		cx = cx-CivLogic.dseg_E200_corruption;
//		int ax = cityOwner.scienceRate();
//		ax = ax+cityOwner.taxRate();
//		ax = ax-10;
//		ax = -ax;
//		ax = ax*cx;
//		ax = ax+5;
//		cx = 10;
//		ax = ax/cx;
//		ax = CivLogic.rangeBound(ax, 0, CivLogic.cityTradeProd_dseg_705E);
//		CivLogic.cityLuxuryProd_dseg_7060 = ax;
		
		CivLogic.cityLuxuryProd_dseg_7060 = CivLogic.rangeBound((cityOwner.luxuryRate()*(CivLogic.cityTradeProd_dseg_705E-CivLogic.dseg_E200_corruption)+5)/10, 0, CivLogic.cityTradeProd_dseg_705E);

		//seg007_293D: non listed
		CivLogic.countCityTaxCollected_dseg_F09A = CivLogic.rangeBound((cityOwner.taxRate()*(CivLogic.cityTradeProd_dseg_705E-CivLogic.dseg_E200_corruption)+5)/10, 0, CivLogic.cityTradeProd_dseg_705E-CivLogic.cityLuxuryProd_dseg_7060-CivLogic.dseg_E200_corruption);               		
		
		//seg007_2976: non listed
		CivLogic.countCityResearchBulbs_dseg_7066 = CivLogic.cityTradeProd_dseg_705E-CivLogic.cityLuxuryProd_dseg_7060-CivLogic.countCityTaxCollected_dseg_F09A-CivLogic.dseg_E200_corruption;
		
		//seg007:2988
		CivLogic.countCityTaxCollected_dseg_F09A += 2*city.getCitySpecialistCount(CitySpecialistType.TAXMAN);
		
		//seg007:299A
		CivLogic.countCityResearchBulbs_dseg_7066 += 2*city.getCitySpecialistCount(CitySpecialistType.SCIENTIST);
		
		//seg007:29AC
		CivLogic.cityLuxuryProd_dseg_7060 += 2*city.getCitySpecialistCount(CitySpecialistType.ENTERTAINER);
		
		if(city.has(CityImprovement.MARKETPLACE)) {
			//seg007_29D0:
			CivLogic.cityLuxuryProd_dseg_7060 += CivLogic.cityLuxuryProd_dseg_7060/2;
			CivLogic.countCityTaxCollected_dseg_F09A += CivLogic.countCityTaxCollected_dseg_F09A/2; 
		}
		//seg007_29E8:
		if(city.has(CityImprovement.BANK)) {
			//seg007_2FB8:
			CivLogic.cityLuxuryProd_dseg_7060 += CivLogic.cityLuxuryProd_dseg_7060/2;
			CivLogic.countCityTaxCollected_dseg_F09A += CivLogic.countCityTaxCollected_dseg_F09A/2; 
		}
		//seg007_2A13:
		var_multi1 = CivLogic.countCityResearchBulbs_dseg_7066;
		if(city.has(CityImprovement.LIBRARY)) {
			//seg007_2A2C:
			var_multi1 += CivLogic.countCityResearchBulbs_dseg_7066/2;
			if(CivLogic.isWonderApplicable(cityOwner, WonderType.NEWTON_COLLEGE)) {
				//seg007_2A50:
				var_multi1 += CivLogic.countCityResearchBulbs_dseg_7066/3;
			}
		}

		//seg007_2A5D:
		if(city.has(CityImprovement.UNIVERSITY)) {
			//seg007_2A70:
			var_multi1 += CivLogic.countCityResearchBulbs_dseg_7066/2;
			if(CivLogic.isWonderApplicable(cityOwner, WonderType.NEWTON_COLLEGE)) {
				//seg007_2A94:
				var_multi1 += CivLogic.countCityResearchBulbs_dseg_7066/3;
			}
		}
		
		//seg007_2AA1:
		if(city.has(WonderType.COPERNICUS_OBSERVATORY)) {
			//seg007_2AB5:
			var_multi1 += var_multi1;
		}
		
		//seg007_2ABD:
		CivLogic.countCityResearchBulbs_dseg_7066 = var_multi1;

		CivLogic.city_happyCitizenCount_dseg_7062 = 0;
		if(cityOwner.equals(gs.player())) {
			//seg007_2AD6:
			CivLogic.city_unhappyCitizenCount_dseg_7064 = city.actualSize() + gs.getDifficultyLevel().getID() - 6;
			
		} else {
			//seg007_2AF4:
			CivLogic.city_unhappyCitizenCount_dseg_7064 = city.actualSize() - 3;
		}
		int var_unhappyCitizenCount = CivLogic.city_unhappyCitizenCount_dseg_7064;
		
		//seg007_2B0B:
		int var_40 = 116;
		CivLogic.adjustCitizenHappiness(city, var_specialistCount);
		if(arg_processMode == 1) {
			//seg007_2B2D:
			if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
				//seg007_2B37:
				var_40 = 116;
				CivDosRenderer.drawCitizensInCityScreen(bi, city, 100, var_40, var_specialistCount, 92);
				var_40 += 16;
				gfx.setColor(Palette.defaultColors.get(1));
				gfx.drawLine(100, var_40-2, 222, var_40-2);
			}
		}
		
		//seg007_2B75:
		CivLogic.city_happyCitizenCount_dseg_7062 = CivLogic.cityLuxuryProd_dseg_7060 / 2;
		CivLogic.adjustCitizenHappiness(city, var_specialistCount);
		if(arg_processMode == 1) {
			//seg007_2BA2:
			if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
				//seg007_2BAC:
				if(CivLogic.city_happyCitizenCount_dseg_7062 != 0) {
					//seg007_2BB6:
					CivDosRenderer.drawCitizensInCityScreen(bi, city, 100, var_40, var_specialistCount, 92);
					BufferedImage luxuryIcon = ResourceManager.getSprite("city.resource.luxury.");
					gfx.drawImage(luxuryIcon, 208, 4+var_40, null);
					var_40 += 16;
					gfx.setColor(Palette.defaultColors.get(1));
					gfx.drawLine(100, var_40-2, 222, var_40-2);
				}
			}
		}
		
		//seg007_2C06:
		int var_4 = 208;
		if(city.has(CityImprovement.COLOSSEUM)) {
			//seg007_2C1E:
			CivLogic.city_unhappyCitizenCount_dseg_7064 -= 3;
			if(arg_processMode == 1) {
				//seg007_2C37:
				if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
					//seg007_2C41:
					BufferedImage icon = ResourceManager.getSprite("city.improvement.icon.colosseum.");
					gfx.drawImage(icon, var_4, var_40, null);
					var_4 -= 16;
				}
			}
		}
		//seg007_2C57:
		if(cityOwner.knows(Advance.RELIGION)) {
			//seg007_2C6F
			int ax = 0;
			if(city.has(CityImprovement.CATHEDRAL)) {
				//seg007_2C82:
				if(CivLogic.isWonderApplicable(cityOwner, WonderType.MICHELANGELO_CHAPEL)) {
					//seg007_2C9A:
					ax = 6;
				} else {
					ax = 4;
				}
			}
			//seg007_2CA9:
			CivLogic.city_unhappyCitizenCount_dseg_7064 -= ax;
			if(arg_processMode == 1) {
				//seg007_2CC1:
				if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
					//seg007_2CCB:
					if(city.has(CityImprovement.CATHEDRAL)) {
						//seg007_2CDE:
						BufferedImage icon = ResourceManager.getSprite("city.improvement.icon.cathedral.");
						gfx.drawImage(icon, var_4, var_40, null);
						var_4 -= 16;
					}
				}
			}
		}
		//seg007_2CF4:
		if(city.has(CityImprovement.TEMPLE)) {
			//seg007_2D06:
			if(cityOwner.knows(Advance.MYSTICISM)) {
				//seg007_2D1E:
				CivLogic.city_unhappyCitizenCount_dseg_7064 -= 2;
			} else {
				//seg007_2D26:
				if(cityOwner.knows(Advance.CEREMONIAL_BURIAL)) {
					//seg007_2D3E:
					CivLogic.city_unhappyCitizenCount_dseg_7064 --;
				}
			}
			//seg007_2D42:
			if(CivLogic.isWonderApplicable(cityOwner, WonderType.ORACLE)) {
				//seg007_2D5A:
				int ax = 1;
				if(cityOwner.knows(Advance.MYSTICISM)) {
					//seg007_2D72:
					ax = 2;
				}
				//seg007_2D7B:
				CivLogic.city_unhappyCitizenCount_dseg_7064 -= ax;
			}
			//seg007_2D7F:
			if(arg_processMode == 1) {
				//seg007_2D93:
				if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
					//seg007_2D9D:
					BufferedImage icon = ResourceManager.getSprite("city.improvement.icon.temple.");
					gfx.drawImage(icon, var_4, var_40, null);
					var_4 -= 16;
				}
			}
		}
		//seg007_2DB3:
		CivLogic.adjustCitizenHappiness(city, var_specialistCount);
		if(arg_processMode == 1) {
			//seg007_2DD5:
			if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
				//seg007_2DDF:
				if(city.has(CityImprovement.TEMPLE)
						|| city.has(CityImprovement.COLOSSEUM)
						|| city.has(CityImprovement.CATHEDRAL)) {
					//seg007_2DDF:
					CivDosRenderer.drawCitizensInCityScreen(bi, city, 100, var_40, var_specialistCount, 92);
					var_40 += 16;
					gfx.setColor(Palette.defaultColors.get(1));
					gfx.drawLine(100, var_40-2, 222, var_40-2);
				}
			}
		}
		
		//seg007_2E2B:
		var_4 = 208;
		if(!cityOwner.government().equals(GovernmentType.DEMOCRACY)
				&& !cityOwner.government().equals(GovernmentType.REPUBLIC)) {
			//seg007_2E40:
			//int var_newUnitID = 0;
			//while(var_newUnitID<2) {
				//seg007_2E57:
				if(CivLogic.city_unhappyCitizenCount_dseg_7064 != 0) {
					//seg007_2E61:
					if(gs.unitType(city.getUnknown_citybyte27()&0x3F)!=null
							&& gs.unitType(city.getUnknown_citybyte27()&0x3F).attack()>0) {
						//seg007_2E77:
						CivLogic.city_unhappyCitizenCount_dseg_7064--;
					}
				}
				//2nd loop iteration
				//seg007_2E57:
				if(CivLogic.city_unhappyCitizenCount_dseg_7064 != 0) {
					//seg007_2E61:
					if(gs.unitType(city.getUnknown_citybyte28()&0x3F)!=null
							&& gs.unitType(city.getUnknown_citybyte28()&0x3F).attack()>0) {
						//seg007_2E77:
						CivLogic.city_unhappyCitizenCount_dseg_7064--;
					}
				}
			//}
			//seg007_2EA3:
			Unit unit = CivLogic.getUnitAt(gs, var_X,  var_Y);
			Unit var_multi1_unit = unit;
			Unit var_newUnitId = unit;
			//seg007_2EBB:
			while(var_newUnitId != null) {
				//seg007_2EC5:
				if(var_newUnitId.getType().attack()>0) {
					//seg007_2EE9:
					if(CivLogic.city_unhappyCitizenCount_dseg_7064>0) {
						//seg007_2EF3:
						CivLogic.city_unhappyCitizenCount_dseg_7064--;
					}
					//seg007_2EF7:
					if(arg_processMode == 1) {
						//seg007_2F0B:
						if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
							//seg007_2F15:
							BufferedImage unitimg = ImageUtils.getCivUnitIcon((SVEUnit) var_newUnitId);
							gfx.drawImage(unitimg, var_4, var_40-1, null);
							var_4 -= 2;
						}
					}
				}
				//seg007_2F4E:
				var_newUnitId = ((SVEUnit)var_newUnitId).nextInStack();
				if(var_newUnitId!=null && var_newUnitId.equals(var_multi1_unit)) {
					//seg007_2F76:
					var_newUnitId = null;
				}
			}
			//seg007_2F7F: -> 3020
		} else {
			//seg007_2F82:
			int var_multi1_sadPerUnit = CivLogic.isWonderApplicable(cityOwner, WonderType.WOMEN_SUFFRAGE)?0:1;
			if(cityOwner.government().equals(GovernmentType.DEMOCRACY)) {
				//seg007_2FAD:
				var_multi1_sadPerUnit++;
			}
			//seg007_2FB1:
			if(var_multi1_sadPerUnit != 0) {
				//seg007_2FBB:
				CivLogic.city_unhappyCitizenCount_dseg_7064 += CivLogic.dseg_64C8_away_unit_counter * var_multi1_sadPerUnit;
				if(arg_processMode == 1) {
					//seg007_2FDB:
					if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
						//seg007_2FE5:
						BufferedImage sadIcon = ResourceManager.getSprite("city.resource.unhappiness.");
						for(int i=0;i<CivLogic.dseg_64C8_away_unit_counter * var_multi1_sadPerUnit;i++) {
							//seg007_3003:
							gfx.drawImage(sadIcon, var_4, 4+var_40, null);
							var_4 -= 2;
						}
					}
				}
			}
		}
		//seg007_3020:
		CivLogic.adjustCitizenHappiness(city, var_specialistCount);
		if(arg_processMode == 1) {
			//seg007_3042:
			if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
				//seg007_304C:
				CivDosRenderer.drawCitizensInCityScreen(bi, city, 100, var_40, var_specialistCount, 92);
				var_40 += 16;
				gfx.setColor(Palette.defaultColors.get(1));
				gfx.drawLine(100, var_40-2, 222, var_40-2);
			}
		}
		//seg007_3085:
		int var_multi1_content = CivLogic.city_happyCitizenCount_dseg_7062 - CivLogic.city_unhappyCitizenCount_dseg_7064;
		if(CivLogic.isWonderApplicable(cityOwner, WonderType.HANGING_GARDENS)) {
			//seg007_30A8:
			CivLogic.city_happyCitizenCount_dseg_7062++;
		}
		//seg007_30AC:
		if(CivLogic.isWonderApplicable(cityOwner, WonderType.CURE_FOR_CANCER)) {
			//seg007_30C4:
			CivLogic.city_happyCitizenCount_dseg_7062++;
		}
		//seg007_30C8:
		if(city.has(WonderType.SHAKESPEARE_THEATRE)) {
			//seg007_30DC:
			CivLogic.city_unhappyCitizenCount_dseg_7064 = 0;
		}
		//seg007_30E2:
		if(CivLogic.isWonderApplicable(cityOwner, WonderType.JSBACH_CATHEDRAL)) {
			//seg007_30FA:
			assert gs.wonder(WonderType.JSBACH_CATHEDRAL.getId()) != null;
			assert gs.wonder(WonderType.JSBACH_CATHEDRAL.getId()).getHostCity() != null;
			if(gs.landMassID(gs.wonder(WonderType.JSBACH_CATHEDRAL.getId()).getHostCity().getLocation())
					== gs.landMassID(city.getLocation())){
				//seg007_3132:
				CivLogic.city_unhappyCitizenCount_dseg_7064 -= 2;
			}
		}
		//seg007_3137:
		CivLogic.adjustCitizenHappiness(city, var_specialistCount);
		if(arg_processMode == 1) {
			//seg007_3042:
			if(CivLogic.dseg_2496_cityViewActiveTab == 1) {
				if(var_multi1_content != CivLogic.city_happyCitizenCount_dseg_7062 - CivLogic.city_unhappyCitizenCount_dseg_7064){
					//seg007_3173:
					CivDosRenderer.drawCitizensInCityScreen(bi, city, 100, var_40, var_specialistCount, 92);
//					seg007:318C 10C                 mov     ax, 15
//					seg007:318F 10C                 push    ax
					
//					seg007:3190 10E                 mov     ax, [bp+var_40]
//					seg007:3193 10E                 add     ax, 5           ; Add
//					seg007:3196 10E                 push    ax
					
//					seg007:3197 110                 mov     ax, 190
//					seg007:319A 110                 push    ax
					
//					seg007:319B 112                 mov     ax, offset aWonders ; "WONDERS"
//					seg007:319E 112                 push    ax
					
//					seg007:319F 114                 call    drawStringFlat_str_x_y_color ; Call Procedure
//					seg007:319F
//					seg007:31A4 114                 add
					CivDosRenderer.drawString("WONDERS", gfx, 1, Palette.defaultColors.get(15), 190, var_40+5);
					var_40 += 16;
					gfx.setColor(Palette.defaultColors.get(1));
					gfx.drawLine(100, var_40-2, 222, var_40-2);
				}
			}
		}
		
		//seg007_31C7:
		//if debug switches 2 is disabled, set happy and unhappy citizens to 0
		
		//seg007_31DA:
		int var_4C = 0;
		CivLogic.dseg_EDD8_unk27_28_counter = 0;
		CivLogic.dseg_EDD6 = 0;
		CivLogic.dseg_E216_unitShieldMaintenanceCost = 0;
		var_loopCounter = 0;
		{
			//seg007_31F6:
			while(var_loopCounter < 2) {
				//seg007_3200:
				if( ( (var_loopCounter==0?city.getUnknown_citybyte27():city.getUnknown_citybyte28())&0xFF) != 0xFF) {
					//seg007_3216:
					CivLogic.dseg_EDD8_unk27_28_counter++;
				}
				var_loopCounter++;
			}
			//seg007_321D:
			if(city.actualSize() < CivLogic.dseg_EDD8_unk27_28_counter) {
				//seg007_3233:
				CivLogic.dseg_E216_unitShieldMaintenanceCost = CivLogic.dseg_EDD8_unk27_28_counter - city.actualSize(); 
			}
			//seg007_324A:
			int var_stockAreaWidth = 8;
			int var_stockAreaHeight = 69;
			int var_F6 = 100;
			int var_FC = 116;
			var_loopCounter = 0;
			
			//seg007_326F:
			while(var_loopCounter < 128) {
				//seg007_327A:
				SVEUnit unit = (SVEUnit) cityOwner.unit(var_loopCounter);
				if(unit!=null && unit.exists()) {
					//seg007_3296:
					if(unit.getHome().equals(city)) {
						//seg007_32B5:
						if(!unit.getType().getUnitTypeCode().equals(UnitTypeCode.DIPLOMAT)
								&& !unit.getType().getUnitTypeCode().equals(UnitTypeCode.CARAVAN)) {
							//seg007_32D1:
							CivLogic.dseg_EDD8_unk27_28_counter++;
							if(city.actualSize()<CivLogic.dseg_EDD8_unk27_28_counter
								||
								//seg007_32EB:
								(!cityOwner.government().equals(GovernmentType.ANARCHY)
										&&!cityOwner.government().equals(GovernmentType.DESPOTISM)
										//&& debugSwitches 2 is set...
										)
								) {
								//seg007_3305:
								CivLogic.dseg_E216_unitShieldMaintenanceCost++;
							}
						}
						//seg007_3309:
						if(arg_processMode == 0) {
							//seg007_3314:
							if(CivLogic.cityShieldProd_dseg_705C < CivLogic.dseg_E216_unitShieldMaintenanceCost
									||
									(
											//seg007_3320:
											city.has(CityStatus.RIOT)
											&&
											//seg007_3332:
											((gs.turn()+var_loopCounter)%8==0)
											&&
											//seg007_3341:
											!cityOwner.equals(gs.player())
											&&
											//seg007_334D:
											(cityOwner.government().equals(GovernmentType.REPUBLIC)
													||cityOwner.government().equals(GovernmentType.DEMOCRACY))
									)
								) {
								//seg007_335D:
								var_bestNeighbourSquareID = -1;
								int var_bestCityBaseTrade = -1;
								int var_spyCityCount = 0;
								
								//seg007_3375:
								while(var_spyCityCount < 0x80) {
									//seg00_3380:
									SVEUnit unit2 = (SVEUnit) cityOwner.unit(var_loopCounter);
									if(unit2!=null && unit2.exists()) {
										//seg007_339C:
										if(unit2.getHome().equals(city)) {
											//seg007_33BB:
											if(!unit2.getType().getUnitTypeCode().equals(UnitTypeCode.DIPLOMAT)
													&& !unit2.getType().getUnitTypeCode().equals(UnitTypeCode.CARAVAN)) {
												//seg007_33D7:
												varOtherDist = CivLogic.distance(city.getLocation().x(), unit2.getLocation().x(), city.getLocation().y(), unit2.getLocation().y());
												if(varOtherDist > var_bestCityBaseTrade) {
													//seg007_3426:
													var_bestCityBaseTrade = varOtherDist;
													var_bestNeighbourSquareID = var_spyCityCount;
												}
											}
										}
									}
									
									
									//seg007_3371:
									var_spyCityCount++;
								}
								//seg007_3439:
								if(CivLogic.cityShieldProd_dseg_705C >= CivLogic.dseg_E216_unitShieldMaintenanceCost) {
									//seg007_3445:
									if(CivLogic.dseg_8F98 != 0
										&&
										//seg007_344F:
										cityOwner.techCount() < gs.player().techCount()
											) {
										//	//seg007_3468:
										CivLogic.dseg_EA62 = -999;
									} else {
										//seg007_3471:
										if(var_bestCityBaseTrade > 0) {
											//seg007_347B:
											if(cityOwner.unit(var_bestNeighbourSquareID).getType().category().equals(UnitCategory.LAND)) {
												//seg007_349F:
												if(!cityOwner.unit(var_bestNeighbourSquareID).getType().getUnitTypeCode().equals(UnitTypeCode.SETTLER)) {
													//seg007_34BB:
													cityOwner.unit(var_bestNeighbourSquareID).delete();
													city.set(CityStatus.RIOT, false);
												}
											}
										}
									}
								} else {
									//seg007_34DB:
									if(gs.player().equals(cityOwner)) {
										//seg007_34E7:
										//TODO DISBANDING UNSUPPORTED UNIT DIALOG
									}
									//seg007_3559
									//DELETE UNIT AND
									cityOwner.unit(var_bestNeighbourSquareID).delete();
									//LOOP BACK TO TOP LOOP seg007_31DA
									continue;
								}
							}
						}
						//seg007_356C:
						if(arg_processMode == 1) {
							//seg007_3580:
							if(!unit.getType().getUnitTypeCode().equals(UnitTypeCode.DIPLOMAT)
									&& !unit.getType().getUnitTypeCode().equals(UnitTypeCode.CARAVAN)) {
								//seg007_359C:
								if(CivLogic.dseg_E216_unitShieldMaintenanceCost != 0) {
									//seg007_35A6:
									BufferedImage shieldicon = ResourceManager.getSprite("city.resource.shield.");
									gfx.drawImage(shieldicon, 8+var_stockAreaWidth,  12+var_stockAreaHeight, null);
								}
								//seg007_35C2:
								if(unit.getType().getUnitTypeCode().equals(UnitTypeCode.SETTLER)) {
									//seg007_35DE:
									BufferedImage foodicon = ResourceManager.getSprite("city.resource.food.");
									gfx.drawImage(foodicon, var_stockAreaWidth,  12+var_stockAreaHeight, null);
									if(cityOwner.government().ordinal()>=2) {
										//seg007_3606:
										gfx.drawImage(foodicon, var_stockAreaWidth+2,  12+var_stockAreaHeight, null);
									}
								} else {
									//seg007_3624:
									if(CivLogic.isWonderApplicable(cityOwner, WonderType.WOMEN_SUFFRAGE)) {
										//seg007_364F:
										var_multi1++;
									}
									//seg007_3653:
									if(cityOwner.government().ordinal()>=4) {
										//seg007_3663:
										if(var_multi1 != 0) {
											//seg007_366D:
											if(unit.getType().category().equals(UnitCategory.AIR)
													||
													//seg007_3691:
													unit.getLocation().x() != city.getLocation().x()
													||
													//seg007_36BA:
													unit.getLocation().y() != city.getLocation().y()
													) {
												//seg007_36E3:
												BufferedImage sadicon = ResourceManager.getSprite("city.resource.unhappiness.");
												gfx.drawImage(sadicon, var_stockAreaWidth,  12+var_stockAreaHeight, null);
												if(var_multi1>1) {
													//seg007_3705:
													gfx.drawImage(sadicon, 2+var_stockAreaWidth,  12+var_stockAreaHeight, null);
												}
											}
										}
									}
								}
							}
							//seg007_3720:
							BufferedImage uniticon = ImageUtils.getCivUnitIcon(cityOwner.getID(), unit.getType().getID());
							gfx.drawImage(uniticon, var_stockAreaWidth, var_stockAreaHeight, null);
							CivDosRenderer.drawPointInCityMap(bi, gs, unit.getLocation().x(), unit.getLocation().y(), 7);
							var_stockAreaWidth += 0x10;
							if(var_stockAreaWidth >= 0x70) {
								//seg007_3790:
								var_stockAreaWidth = 8;
								var_stockAreaHeight += 0x10;
								if(var_stockAreaHeight > 0x55) {
									//seg007_37A5:
									var_stockAreaHeight -= 0x18;
								}
							}
						}
					}
				}
				int[] var_72 = new int[0x12];
				//seg007_37AA:
				if(var_4C < 0x12) {
					//seg007_37B3:
					if(CivLogic.dseg_2496_cityViewActiveTab == 0) {
						//seg007_37BD:
						if(unit!=null && unit.exists()) {
							//seg007_37D9:
							if(arg_processMode == 1) {
								//seg007_37ED:
								if(unit.getLocation().equals(city.getLocation())) {
									//seg007_382F:
									
									//gfx.drawImage(ImageUtils.getCivUnitIcon(unit),var_F6, var_FC, null);
									ImageUtils.drawUnit((AbstractSVEUnit) unit, gfx, var_F6, var_FC);
									
									String cityCode = unit.getHome().getName().substring(0, 3)+".";
									CivDosRenderer.drawString(cityCode, gfx, 1, Palette.defaultColors.get(0), var_F6, var_FC+0xF +6);
									var_72[var_4C++] = var_loopCounter;
									var_F6 += 0x12;
									if(var_4C%6 == 0) {
										var_F6 = 0x64;
										var_FC += 0x10;
									}
								}
							}
						}
					}
				}
				//seg007_38C2:
				var_loopCounter++;
			}
			//seg007_38C5:
			if(arg_processMode == 0) {
				//seg007_38D0:
				if(CivLogic.cityShieldProd_dseg_705C<CivLogic.dseg_EDD8_unk27_28_counter) {
					//seg007_38DC:
					CivLogic.dseg_F2E2 += (CivLogic.dseg_EDD8_unk27_28_counter - CivLogic.cityShieldProd_dseg_705C) * 5;
				}
				//seg007_38EC:
				CivLogic.dseg_F2E2 += CivLogic.rangeBound(CivLogic.dseg_EDD8_unk27_28_counter, 0, city.actualSize());
				int ax;
				if(city.has(CityImprovement.MARKETPLACE)) {
					//seg007_3920:
					ax = 5;
				} else {
					//seg007_3926:
					ax = 7;
				}
				CivLogic.dseg_EA62 -= CivLogic.dseg_64C8_away_unit_counter * (ax - cityOwner.militaryAttitude().value());
			}
			//seg007_3948:
			if(arg_processMode == 1) { // heavy duty rendering! strap your seat belts...
				//seg007_395C:
				CivDosRenderer.drawBlueDialogBackground(211, 1, 317, 97, gfx, false);
				int var_spyCityCount = 2;
				var_Y = 2;
				int var_3E = 0;
				int var_B2 = 0;
				if(var_D8 == 0) {
					//seg007_399C:
					int var_improvementTypeID = 0;
					while(var_improvementTypeID < 21) {
						//seg007_39B3:
						if(city.equals(gs.wonder(var_improvementTypeID).getHostCity())) {
							//seg007_39C5:
							CivDosRenderer.drawString(FontUtils.truncateStringToPixelLength(gs.wonder(var_improvementTypeID).getName().toUpperCase(), 1, 63), gfx, 1, Palette.defaultColors.get(15), 253, var_Y+2+6);
							
							//seg007_3A38:
							BufferedImage wicon = ResourceManager.getSprite("wonder.icon."+gs.wonder(var_improvementTypeID).getType().getCode()+".");
							gfx.drawImage(wicon, var_spyCityCount%2==0?233:213, var_Y-2, null);
							
							var_Y += 6;
							var_spyCityCount++;
							var_3E++;
						}
						//seg007_39A5:
						var_improvementTypeID++;
					}
				}
				//seg007_3A56:
				var_FA = 0;
				int var_improvementTypeID = var_D8;
				while(var_improvementTypeID < 24) {
					//seg007_3A75:
					if(city.has(CityImprovement.getById(var_improvementTypeID))) {
						//seg007_3A9F:
						if(var_spyCityCount >= 16) {
							//seg007_3AA9:
							var_C8 |= 2;
							var_C8 &= 0xFFFE;
							var_FA = var_improvementTypeID;
							break;
						} else {
							//seg007_3ABF:
							BufferedImage sicon = ResourceManager.getSprite("city.resource.tax.");
							gfx.drawImage(sicon, 309,  var_Y+1, null);
							
							CivDosRenderer.drawString(FontUtils.truncateStringToPixelLength(CityImprovement.getById(var_improvementTypeID).getLocalizedName(gs.getVersion().language()).toUpperCase(), 1, 56), gfx, 1, Palette.defaultColors.get(15), 253, var_Y+2+6);
							
							BufferedImage bicon = ResourceManager.getSprite("city.improvement.icon."+CityImprovement.getById(var_improvementTypeID).getCode()+".");
							gfx.drawImage(bicon, var_spyCityCount%2==0?233:213, var_Y-2, null);

							//seg007_3B3E:
							var_Y += 6;
							var_spyCityCount++;
						}
					}
					var_improvementTypeID++;
				}
				//seg007_3B59:
				gfx.setColor(Palette.defaultColors.get(0));
				gfx.drawLine(231, 0, 250, 0);
				gfx.setColor(Palette.defaultColors.get(1));
				gfx.drawLine(231, 1, 250, 1);

				if((var_C8 & 2) != 0) {
					//seg007_3B9B:
					CivDosRenderer.drawPushButton(287, 88, 315, 96, bi, "MORE", Palette.defaultColors.get(9));
				}
				//seg007_3BBB:
				CivDosRenderer.replaceColor(bi, 309, 2, 8, 96, 14, 12);
				
				//seg007_3BF1:
				//seg007_3C0C:
				int var_costBase = city.getCurrentProduction().getCostBase();
				
				//seg007_3C2A:
				int max = 99;
				int min = (city.getShieldStock()-1)/100+1;
				int val = (var_costBase-1)/10+1;
				int var_columns = CivLogic.rangeBound(val, min, max);
				
				var_multi1 = 80 / var_16_min_2xDifficulty;
				
//				seg007:3C6A 10C                 mov     ax, [bp+var_16_min_2xDifficulty]
				//int ax = var_16_min_2xDifficulty;
//						seg007:3C6D 10C                 imul    [bp+var_multi1] ; Signed Multiply
				//ax *= var_multi1;
//						seg007:3C71 10C                 mov     cx, ax
				//int cx = ax;
//						seg007:3C73 10C                 mov     ax, [bp+var_42]
				//ax = var_42;
//						seg007:3C76 10C                 mov     dx, cx
				//int dx = cx;
//						seg007:3C78 10C                 mov     cl, 3
				//cx = 3;
//						seg007:3C7A 10C                 shl     ax, cl          ; Shift Logical Left
				//ax <<= cx; // *8
//						seg007:3C7C 10C                 sub     ax, 8           ; Integer Subtraction
				//ax -= 8;
//						seg007:3C7F 10C                 mov     cx, dx
				//cx = dx;
//						seg007:3C81 10C                 cwd                     ; AX -> DX:AX (with sign)
//						seg007:3C82 10C                 mov     bx, [bp+var_42]
//						seg007:3C85 10C                 idiv    bx              ; Signed Divide
				//ax /= var_42;
//						seg007:3C87 10C                 add     ax, cx          ; Add
				//ax += cx;
//						seg007:3C89 10C               
				//var_F2 = ax;
				
				var_stockAreaWidth = ((var_columns*8)-8)/var_columns + var_16_min_2xDifficulty*var_multi1;

				var_stockAreaHeight = (((var_costBase-1)/var_columns)*8)+8;

				gfx.setColor(Palette.defaultColors.get(1));
				gfx.fillRect(230, 99, var_stockAreaWidth+3, var_stockAreaHeight+19);
				
				if(cityOwner.equals(gs.player())) {
					//seg007_3CD7:
					CivDosRenderer.drawPushButton(231, 106, 263, 114, bi, city.has(CityStatus.AUTO_BUILD)?"AUTO.":"CHANGE", Palette.defaultColors.get(9));
					CivDosRenderer.drawPushButton(294, 106, 311, 114, bi, "BUY", Palette.defaultColors.get(9));
				}

				//seg007_3D2F:
				if(city.getCurrentProductionID()<28) {
					//seg007_3D41:
					gfx.drawImage(ImageUtils.getCivUnitIcon(cityOwner.getID(), city.getCurrentProductionID(), false), 264, 100, null);
				} else {
					//seg007_3D6F:
					CivDosRenderer.drawString(FontUtils.truncateStringToPixelLength(city.getCurrentProduction().getListDescription(), 1,  86), gfx, 1, Palette.defaultColors.get(15), 274, 100+6, 100, 1);
				}
				//seg007_3DBB:
				CivDosRenderer.fillRectWithCityPanelBackground(231, 116, var_stockAreaWidth+1, var_stockAreaHeight+1, gfx, 0);
				
				for(var_loopCounter = 0; var_loopCounter<city.getShieldStock() ; var_loopCounter++) {
					//seg007_3DF9:
					gfx.drawImage(ResourceManager.getSprite("city.resource.shield."),
							232+((var_loopCounter%(var_columns*var_16_min_2xDifficulty))*var_multi1)/var_columns,
							117+((var_loopCounter/(var_columns*var_16_min_2xDifficulty))<<3), 
							null);
				}
				//seg007_3E36:
				max = 8;
				min = 1;
				val = 80 / city.actualSize();
				var_multi1 = CivLogic.rangeBound(val, min, max); // food area width in columns

				var_stockAreaHeight = 80 / var_16_min_2xDifficulty;
				gfx.setColor(Palette.defaultColors.get(1));
				gfx.fillRect(2, 106, 91, 12+var_stockAreaHeight*var_16_min_2xDifficulty);
				
				CivDosRenderer.drawString("Food Storage", gfx, 1, Palette.defaultColors.get(15), 8, 6+108);
				
				CivDosRenderer.fillRectWithCityPanelBackground(3, 0x73, 9+var_multi1*city.actualSize(), 2+var_stockAreaHeight*var_16_min_2xDifficulty, gfx, 0);
				
				if(city.has(CityImprovement.GRANARY)) {
					//seg007_3EEC:
					gfx.setColor(Palette.defaultColors.get(1));
					gfx.drawLine(5, 155, 9+var_multi1*city.actualSize(), 155);
				}
				
				//seg007_3F19:
				for(var_loopCounter = 0;
						/*seg007_3F26:*/ var_loopCounter < CivLogic.rangeBound(city.getFoodStock(),0,(city.actualSize()+1)*var_16_min_2xDifficulty);
						var_loopCounter++) {
					//seg007_3F51:
					gfx.drawImage(ResourceManager.getSprite("city.resource.food."),
							4+var_multi1*(var_loopCounter%(city.actualSize()+1)),
							0x74+var_stockAreaHeight*(var_loopCounter/(city.actualSize()+1)), 
							null);
				}
				//seg007_3F90:
				CivDosRenderer.drawBlueDialogBackground(2, 23, 124, 65, gfx, false);
				
				var_stockAreaHeight = 25;
				gfx.setColor(Palette.defaultColors.get(1));
				gfx.fillRect(2,  23,  122,  9);
				CivDosRenderer.drawString("City Resources", gfx, 1, Palette.defaultColors.get(15), 8, 6+var_stockAreaHeight);
				var_stockAreaHeight += 8;
				for(var_improvementTypeID = 0;
						//seg007_3FF8:
						var_improvementTypeID < 3;
						//seg007_3FF4:
						var_improvementTypeID ++) {
					//seg007_4002:
					var_X = 4;
					if(var_improvementTypeID == 0) {
						//seg007_4012:
//					    seg007_4012:
//					    	10C mov     ax, 8
						int ax = 8;
//					    	10C push    ax              ; C
						int arg0 = ax;
//					    	10E mov     ax, 1
						ax = 1;
//					    	10E push    ax
						int arg1 = ax;
//					    	110 mov     ax, 999
						ax = 999;
//					    	110 push    ax              ; C
						int arg2 = ax;
//					    	112 mov     ax, 1Ch
//					    	112 imul    [bp+arg_cityID] ; Signed Multiply
//					    	112 mov     bx, ax
//					    	112 mov     al, CityData.ActualSize[bx]
//					    	112 cbw                     ; AL -> AX (with sign)
						ax = city.actualSize();
//					    	112 shl     ax, 1           ; Shift Logical Left
						ax <<= 1;
//					    	112 mov     cx, ax
						int cx = ax;
//					    	112 mov     ax, [bp+var_settlerFoodCost]
						ax = var_settlerFoodCost;
//					    	112 imul    dseg_F2E6_settler_counter ; Signed Multiply
						ax *= CivLogic.dseg_F2E6_settler_counter;
//					    	112 add     cx, ax          ; Add
						cx += ax;
//					    	112 push    cx
						int arg3 = cx;
//					    	114 mov     bx, [bp+var_ImprovementTypeID]
//					    	114 shl     bx, 1           ; Shift Logical Left
//					    	114 push    cityFoodProd_dseg_705A[bx] ; __int16
						int arg4 = CivLogic.cityFoodProd_dseg_705A;
//					    	116 call    Math_rangeBound_val__min_max ; Call Procedure
						ax = CivLogic.rangeBound(arg4, arg3, arg2);
//					    	116 add     sp, 6           ; Add
//					    	110 mov     cx, ax
						cx = ax;
//					    	110 inc     cx              ; Increment by 1
						cx++;
//					    	110 mov     ax, 74h ; 't'
						ax = 0x74;
//					    	110 mov     dx, 0
//					    	110 idiv    cx              ; Signed Divide
						ax /= cx;
//					    	110 push    ax              ; __int16
						int arg5 = ax;
//					    	112 call    Math_rangeBound_val__min_max ; Call Procedure
						ax = CivLogic.rangeBound(arg5, arg1, arg0);
//					    	112 add     sp, 6           ; Add
//					    	10C mov     [bp+var_multi1], ax
						var_multi1 = ax;
//					    	10C jmp     seg007_408E     ; Jump					
					} else {
						//seg007_4066:
//					    seg007_4066:
//					    	10C mov     ax, 8
//					    	10C push    ax              ; C
//					    	10E mov     ax, 1
//					    	10E push    ax
//					    	110 mov     ax, 116
//					    	110 mov     dx, 0
//					    	110 mov     bx, [bp+var_ImprovementTypeID]
//					    	110 shl     bx, 1           ; Shift Logical Left
//					    	110 mov     cx, cityFoodProd_dseg_705A[bx]
//					    	110 inc     cx              ; Increment by 1
//					    	110 idiv    cx              ; Signed Divide
//					    	110 push    ax              ; __int16
//					    	112 call    Math_rangeBound_val__min_max ; Call Procedure
//					    	112 add     sp, 6           ; Add
//					    	10C mov     [bp+var_multi1], ax
						int resProd = 0;
						switch(var_improvementTypeID) {
						case 1:
							resProd = CivLogic.cityShieldProd_dseg_705C;
							break;
						case 2:
							resProd = CivLogic.cityTradeProd_dseg_705E;
							break;
						default:
							break;
						}
						var_multi1 = CivLogic.rangeBound(116/(resProd+1), 1, 8);
					}
					//seg007_408E:
					for(var_loopCounter = 0;
							//seg007_409B:
							var_loopCounter < (var_improvementTypeID == 0?CivLogic.cityFoodProd_dseg_705A:
								var_improvementTypeID == 1?CivLogic.cityShieldProd_dseg_705C:
									CivLogic.cityTradeProd_dseg_705E);
							//seg007_4097:
							var_loopCounter++) {
						//seg007_40AE:
						String restr = null;
						if(var_improvementTypeID == 0) {
							restr = "food";
							//seg007_40B8:
							if(var_loopCounter == city.actualSize()*2 + var_settlerFoodCost*CivLogic.dseg_F2E6_settler_counter) {
								//seg007_40DB:
								var_X += 4;
							}
						}
						//seg007_40E0:
						if(var_improvementTypeID == 1) {
							restr = "shield";
							//seg007_40EA:
							if(CivLogic.dseg_E216_unitShieldMaintenanceCost != 0) {
								//seg007_40F4:
								if(CivLogic.dseg_E216_unitShieldMaintenanceCost == var_loopCounter) {
									//seg007_4100:
									var_X += 4;
								}
							}
						}
						//seg007_4105:
						if(var_improvementTypeID == 2) {
							restr = "trade";
							//seg007_410F:
							if(CivLogic.cityTradeProd_dseg_705E - CivLogic.dseg_E200_corruption == var_loopCounter) {
								//seg007_411F:
								var_X += 2;
							}
						}
						//seg007_4124:
						BufferedImage resicon = ResourceManager.getSprite("city.resource."+restr+".");
						gfx.drawImage(resicon, var_X, var_improvementTypeID*8+var_stockAreaHeight, null);
						var_X += var_multi1;
					}
					//seg007_4150:
				}
				//seg007_4153:
				var_X = 8;
				var_multi1 = CivLogic.rangeBound(224 / (CivLogic.cityLuxuryProd_dseg_7060+CivLogic.countCityResearchBulbs_dseg_7066+CivLogic.countCityTaxCollected_dseg_F09A+CivLogic.dseg_E200_corruption+1+1), 1, 16);
				var_loopCounter = 0;
				//seg007_4195:
				while(var_loopCounter<CivLogic.cityLuxuryProd_dseg_7060) {
					//seg007_41A1:
					gfx.drawImage(ResourceManager.getSprite("city.resource.luxury."), var_X/2, var_stockAreaHeight+0x18, null);
					var_X += var_multi1;
					//seg007_4191:
					var_loopCounter++;
				}
				//seg007_41CA:
				if(CivLogic.cityLuxuryProd_dseg_7060 != 0) {
					//seg007_41D4:
					var_X += 8;
				}

				//seg007_41D9:
				var_stockAreaWidth = var_X;
				var_loopCounter = 0;
				//seg007_41EE:
				while(var_loopCounter < CivLogic.countCityTaxCollected_dseg_F09A) {
					//seg007_41FA:
					gfx.drawImage(ResourceManager.getSprite("city.resource.tax."), var_X/2, var_stockAreaHeight+0x18, null);
					var_X += var_multi1;
					//seg007_41EA:
					var_loopCounter++;
				}
				//seg007_4223:
				if(CivLogic.countCityTaxCollected_dseg_F09A != 0) {
					//seg007_422D:
					var_X += 8;
				}

				//seg007_4232:
				var_loopCounter = 0;
				//seg007_423F:
				while(var_loopCounter < CivLogic.countCityResearchBulbs_dseg_7066) {
					//seg007_424B:
					gfx.drawImage(ResourceManager.getSprite("city.resource.science."), var_X/2, var_stockAreaHeight+0x18, null);
					var_X += var_multi1;
					//seg007_423B:
					var_loopCounter++;
				}
				//seg007_4274:
				if(CivLogic.cityFoodProd_dseg_705A < city.actualSize()*2 + var_settlerFoodCost*CivLogic.dseg_F2E6_settler_counter) {
					//seg007_4296:
					val = 116 / (city.actualSize()*2 + var_settlerFoodCost*CivLogic.dseg_F2E6_settler_counter + 1);
					var_multi1 = CivLogic.rangeBound(val, 1, 8);
					var_loopCounter = CivLogic.cityFoodProd_dseg_705A;
					//seg007_42DD:
					while(var_loopCounter < city.actualSize()*2 + var_settlerFoodCost*CivLogic.dseg_F2E6_settler_counter) {
						//seg007_4300:
						gfx.drawImage(ResourceManager.getSprite("city.resource.food."), 8+var_multi1*var_loopCounter, var_stockAreaHeight, null);
						//seg007_42D9:
						var_loopCounter++;
					}
					//seg007_431F:
					CivDosRenderer.replaceColor(bi, 8+var_multi1*CivLogic.cityFoodProd_dseg_705A, var_stockAreaHeight, 4+var_multi1*(city.actualSize()*2+CivLogic.dseg_F2E6_settler_counter*var_settlerFoodCost - CivLogic.cityFoodProd_dseg_705A), 8, 15, 0);
				}
				//seg007_436D:
				if(CivLogic.cityShieldProd_dseg_705C < CivLogic.dseg_E216_unitShieldMaintenanceCost) {
					//seg007_4379:
					var_multi1 = CivLogic.rangeBound(116 / (CivLogic.cityShieldProd_dseg_705C+1), 1, 8);
					var_loopCounter = CivLogic.cityShieldProd_dseg_705C;
					//seg007_43A9:
					while(var_loopCounter < CivLogic.dseg_E216_unitShieldMaintenanceCost) {
						//seg007_43B5:
						gfx.drawImage(ResourceManager.getSprite("city.resource.shield."), 8+var_multi1*var_loopCounter, var_stockAreaHeight+8, null);
						//seg007_43A5:
						var_loopCounter++;
					}
					//seg007_43D8:
					CivDosRenderer.replaceColor(bi, 8+var_multi1*CivLogic.cityShieldProd_dseg_705C, 8+var_stockAreaHeight, var_multi1*(CivLogic.dseg_E216_unitShieldMaintenanceCost - CivLogic.cityShieldProd_dseg_705C), 8, 15, 0);
				}
				//seg007_4410:
				if(CivLogic.dseg_E200_corruption != 0) {
					//seg007_441A:
					var_multi1 = CivLogic.rangeBound(116 / (CivLogic.cityTradeProd_dseg_705E+1), 1, 8);
					CivDosRenderer.replaceColor(bi, 6+var_multi1*(CivLogic.cityTradeProd_dseg_705E - CivLogic.dseg_E200_corruption), var_stockAreaHeight + 16, var_multi1 * (CivLogic.dseg_E200_corruption+2), 8, 15, 0);
				}
				//seg007_4476:
				CivDosRenderer.fillRectWithCityPanelBackground(8, 8, 200, 13, gfx, 0);
				int var_F0 = CivDosRenderer.drawCitizensInCityScreen(bi, city, 8, 8, var_specialistCount, 192);
				var_multi1 = CivLogic.rangeBound(0x18+city.actualSize()*8, 0, 0x80);
				var_improvementTypeID = 0;
				
				//seg007_44E2:
				while(var_improvementTypeID < 3) {
					//seg007_44EC:
					SVECity tradeCity = null;
					switch(var_improvementTypeID) {
					case 0:
						tradeCity = city.getTradeCity1();
						break;
					case 1:
						tradeCity = city.getTradeCity2();
						break;
					case 2:
						tradeCity = city.getTradeCity3();
						break;
					default:
						break;
					}
					if(tradeCity != null && ((SVECity) tradeCity).exists()) {
						String str = tradeCity.getName()+":+";
						if(cityOwner.equals(tradeCity.owner())) {
							//seg007_4591:
							str += (city.getBaseTrade() + tradeCity.getBaseTrade() + 4)/16;
						} else {
							//seg007_4541:
							str += (city.getBaseTrade() + tradeCity.getBaseTrade() + 4)/8;
						}
						//seg007_45DE:
						str+= "} ";
						if(CivLogic.dseg_2496_cityViewActiveTab == 2) {
							//seg007_45F8:
							CivDosRenderer.drawPointInCityMap(bi, gs, tradeCity.getLocation().x(), tradeCity.getLocation().y(), 10);
						}
						//seg007_461A:
						if(CivLogic.dseg_2496_cityViewActiveTab == 0) {
							//seg007_4624:
							CivDosRenderer.drawString(str, gfx, 1, Palette.defaultColors.get(10), 98, 6+ 179+6*var_improvementTypeID);
						}
					}
					//seg007_44DE:
					var_improvementTypeID++;
				}
				//seg007_4646:
				if(CivLogic.dseg_2496_cityViewActiveTab == 2) {
					//seg007_4650:
					CivDosRenderer.drawPointInCityMap(bi, gs, city.getLocation().x(), city.getLocation().y(), 15);
				}
				//seg007_4673:
				if(CivLogic.dseg_2496_cityViewActiveTab == 0) {
					//seg007_467D:
					int var_pollutionProd = -20 + CivLogic.cityShieldProd_dseg_705C/CivLogic.dseg_6C18_cityPowerType;
					var_pollutionProd += (city.actualSize()*CivLogic.pollutionFactor_dseg_C7A2)/4;
					
					var_stockAreaHeight = 
							CivLogic.rangeBound(
									100/CivLogic.rangeBound(var_pollutionProd,
											1, 99),
									1, 8);
					
					var_loopCounter = 0;
					//seg007_46F1:
					while(var_loopCounter < var_pollutionProd) {
						//seg007_46FE:
						gfx.drawImage(ResourceManager.getSprite("city.resource.pollution."), 98+var_loopCounter*var_stockAreaHeight, (var_loopCounter&1)+161, null);
						//seg007_46ED:
						var_loopCounter++;
					}
				}
				//seg007_4728:
				if(cityOwner.equals(gs.player()) /*  || Cheat Enabled */ ) {
					//seg007_473E:
					CivDosRenderer.drawPushButton(284, 190, 316, 198, bi, "EXIT", Palette.defaultColors.get(12));
					int var_flag = 0;
					if(city.getCurrentProductionID()>27) {
						//seg007_4776:
						if(city.getCurrentProductionID()>=235) {
							//seg007_4788:
							if(city.has((CityImprovement) city.getCurrentProduction())) {
								//seg007_47B5:
								var_flag = 1;
							}
						}
					}
					//seg007_47BB:
					if(city.getCurrentProductionID()<235 && city.getCurrentProductionID()>27) {
						//seg007_47CD:
						//weird, skipped: cmp     word ptr CityData.Buildings2[bx], -1
						//                jge     short seg007_47DF
						//                seg007_47DF:            ; Jump if Less or Equal (ZF=1 | SF!=OF)
						//                jle     short seg007_47E4
						//seg007_47E4:
						if(city.getCurrentProductionID()>=232) {
							//seg007_47EE:
							if(cityOwner.hasLaunchedSpaceShip()) {
								//seg007_4800:
								var_flag = 1;
							}
						}
					}
					//seg007_4806:
					if((city.getCurrentProductionID()<232 && city.getCurrentProductionID()>27) || city.getCurrentProductionID()<-24) { // Wonders
						//seg007_4818:
						if(gs.wonder((WonderType) city.getCurrentProduction()).getHostCity()!=null) {
							//seg007_4835:
							var_flag = 1;
						}
					}
					//seg007_483B:
					if((city.getCurrentProductionID()>=0 && city.getCurrentProductionID()<28)) { // Units
						//seg007_484D:
						if(cityOwner.units().size()>=127) {
							//seg007_485D:
							var_flag = 1;
						}
					}
					//seg007_4863:
					//HERE STARTS THE INTERACTIVE MANAGEMENT OF CITY SCREEN
				}
				//seg007_5AF1:
			}
			//seg007_5B04:
			
		}

		
		System.out.println("DEBUG: "+city.getName()+" ["+city.getID()+"] has:\r\n"
				+"\t"+CivLogic.dseg_F2E6_settler_counter+" settlers\r\n"
				+"\t"+CivLogic.cityFoodProd_dseg_705A+" food prod\r\n"
				+"\t"+CivLogic.cityShieldProd_dseg_705C+" shield prod\r\n"
				+"\t"+CivLogic.cityTradeProd_dseg_705E+" trade prod\r\n"
				+"\t"+CivLogic.cityLuxuryProd_dseg_7060+" luxury prod\r\n"
				+"\t"+CivLogic.countCityResearchBulbs_dseg_7066+" bulbs\r\n"
				+"\t"+CivLogic.countCityTaxCollected_dseg_F09A+" tax\r\n"
				+"\t"+CivLogic.dseg_E200_corruption+" corruption\r\n"
				);
		//ImageUtils.animate(bi, city.getName().toUpperCase(), 200);
		// TODO: end
	}


}