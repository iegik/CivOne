/*
 * This is a development version of a JCivED source file, reverse-engineered and
 * back-ported from CIV.EXE assembly to Java (from Sid Meier's Civilization for MS-DOS)
 * It covers an incomplete set of utility game functions called throughout the game.
 * darkpandaman @ gmail.com - 31/10/2014
 */

package dd.civ.logic.port;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import dd.civ.data.game.model.generic.Entity;
import dd.civ.data.game.model.generic.AbstractGameState;
import dd.civ.data.game.model.generic.Map;
import dd.civ.data.game.model.generic.MapLocation;
import dd.civ.data.game.model.generic.City;
import dd.civ.data.game.model.generic.Civ;
import dd.civ.data.game.model.generic.Unit;
import dd.civ.data.game.model.generic.UnitType;
import dd.civ.data.game.model.generic.impl.AbstractCity;
import dd.civ.data.game.model.sve.SVECity;
import dd.civ.data.game.model.sve.SVECiv;
import dd.civ.data.game.model.sve.SVEMapEntity;
import dd.civ.data.game.model.sve.SVEUnit;
import dd.civ.data.game.model.sve.SVEUnitType;
import dd.civ.data.game.model.sve.impl.AbstractSVEUnit;
import dd.civ.data.game.model.sve.impl.CivDosGameState;
import dd.civ.data.game.types.Advance;
import dd.civ.data.game.types.CityImprovement;
import dd.civ.data.game.types.CityStatus;
import dd.civ.data.game.types.DifficultyLevel;
import dd.civ.data.game.types.DiplomaticAttitude;
import dd.civ.data.game.types.ExpansionAttitude;
import dd.civ.data.game.types.MilitaryAttitude;
import dd.civ.data.game.types.TerrainImprovementType;
import dd.civ.data.game.types.TerrainType;
import dd.civ.data.game.types.UnitCategory;
import dd.civ.data.game.types.UnitStatus;
import dd.civ.data.game.types.VisibilityType;
import dd.civ.data.game.types.WonderType;
import dd.civ.graphics.CivDosRenderer;
import dd.civ.logic.CivUtils;
import dd.commons.util.Utils;
import dd.jcived.util.ImageUtils;
import dd.jcived.util.Palette;
import dd.jcived.util.ResourceManager;

public class CivLogic {

	public static Object[][] defaultAttitudes = {
		{ DiplomaticAttitude.get(0), ExpansionAttitude.get(0), MilitaryAttitude.get(0) },

		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get( 1), MilitaryAttitude.get( 1) },//Caesar
		{ DiplomaticAttitude.get(-1), ExpansionAttitude.get(-1), MilitaryAttitude.get( 1) },//Hammurabi
		{ DiplomaticAttitude.get( 1), ExpansionAttitude.get(-1), MilitaryAttitude.get( 1) },//Frederick
		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get( 0), MilitaryAttitude.get( 1) },//Ramesses
		{ DiplomaticAttitude.get(-1), ExpansionAttitude.get( 0), MilitaryAttitude.get( 1) },//Abe Lincoln
		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get( 1), MilitaryAttitude.get(-1) },//Alexander
		{ DiplomaticAttitude.get(-1), ExpansionAttitude.get(-1), MilitaryAttitude.get( 0) },//M.Gandhi

		{ DiplomaticAttitude.get(0), ExpansionAttitude.get(0), MilitaryAttitude.get(0) },

		{ DiplomaticAttitude.get( 1), ExpansionAttitude.get( 0), MilitaryAttitude.get(-1) },//Stalin
		{ DiplomaticAttitude.get( 1), ExpansionAttitude.get( 0), MilitaryAttitude.get( 0) },//Shaka
		{ DiplomaticAttitude.get( 1), ExpansionAttitude.get( 1), MilitaryAttitude.get( 1) },//Napoleon
		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get(-1), MilitaryAttitude.get( 1) },//Montezuma
		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get( 0), MilitaryAttitude.get( 1) },//Mao Tse Tung
		{ DiplomaticAttitude.get( 0), ExpansionAttitude.get( 1), MilitaryAttitude.get( 0) },//Elizabeth I
		{ DiplomaticAttitude.get( 1), ExpansionAttitude.get( 1), MilitaryAttitude.get(-1) } //Genghis Khan

	};

	public static int getNormalizedSign(int n) {
		return n>0?1:n<0?-1:0;
	}
	public static int moveUnitGoto(AbstractSVEUnit unit) {
		if(unit!=null) {
			int gmux = unit.getDestination().x() - unit.getLocation().x();
			int gmuy = unit.getDestination().y() - unit.getLocation().y();
			if(unit.owner().equals(((Entity)unit).gamesave().player())
					&& Math.abs(gmux)<=1
					&& Math.abs(gmuy)<=1) {
				if(Math.abs(gmux)>=40) {
					gmux = -getNormalizedSign(gmux);
				} else {
					gmux = getNormalizedSign(gmux);
				}
				gmuy = getNormalizedSign(gmuy);
				unit.setPropertyValue("gotox", (Short)(short)0xFF);
				int nid = 1;
				while(nid<=8) {
					if(gmux==CivUtils.relCitySquareX3[nid]
							&& gmuy==CivUtils.relCitySquareY3[nid]) {
						return nid;
					}
				}
				return 0;
			} else {
				if(!unit.category().equals(UnitCategory.AIR)) {
					int var_E = 0;
					if((Math.abs(gmux)<7||Math.abs(gmux)>73)
							&& Math.abs(gmuy)<7) {
						int var_next = CivLogic.findNextPathSquare(unit, 999, unit.getDestination().x(), unit.getDestination().y());
						if(var_next != -1) {
							return var_next;
						}
						var_E = 1;
					}
					// seg016_1CE
					if(hasReachableGoto(unit)||(var_E==0)) {
						int var_next = CivLogic.findNextPathSquare(unit, 999, unit.getDestination().x(), unit.getDestination().y());
						if(var_next != -1) {
							return var_next;
						}
					}
				} else {
				}
				// seg016_1FE
				int di = Math.abs(gmuy);
				int agmux = Math.abs(gmux);
				int agmuy = Math.abs(gmuy);
				int distanceToGoTo = Math.abs(Math.abs(gmux)<=agmuy?agmuy:agmux)+agmux+di;
				if(gmux==0&&gmuy==0) {
					unit.setPropertyValue("goto_next_direction", (Short)(short)0xFF);
					unit.setPropertyValue("gotox", (Short)(short)0xFF);
					unit.setPropertyValue("remaining_moves", (Short)(short)0);
					return -1;
				}
				// seg016_298
				int bestMoveScore = 9999;
				int next = 0;

				boolean roadUnderUnit = ((Entity)unit).gamesave().hasTerrainImprovement(unit.getLocation().x(), unit.getLocation().y(), false, TerrainImprovementType.ROAD);

				boolean unitNextToEnemy = checkSquareNextToEnemyUnit(unit.owner(), unit.getLocation().x(), unit.getLocation().y());

				for(int nid=1;nid<9;nid++) {
					//seg016_323
					int nx = unit.getLocation().x() + CivUtils.relCitySquareX3[nid];
					int ny = unit.getLocation().y() + CivUtils.relCitySquareY3[nid];
					int gmux2 = gmux - CivUtils.relCitySquareX3[nid];
					int gmuy2 = gmuy - CivUtils.relCitySquareY3[nid];
					agmuy = Math.abs(gmuy2);
					agmux = Math.abs(gmux2);
					int agmuy2 = Math.abs(gmuy2);
					int distanceFromNeighbourToGoTo = Math.abs(Math.abs(gmux2)>agmuy2?gmux2:gmuy2)+agmux+agmuy;
					// If Civ is AI (not human), or distance from neighbour to Goto is smaller than from current square
					if(!unit.owner().equals(((Entity)unit).gamesave().player())||distanceFromNeighbourToGoTo<=distanceToGoTo) {
						TerrainType ntt = ((Entity)unit).gamesave().getTerrain(nx, ny);
						Civ nciv = CivLogic.whatCivOccupies(((Entity)unit).gamesave(), nx, ny);
						if(
								(
										(
												// if no Civ on next square, or same as current unit's Civ
												( nciv==null || nciv.equals(unit.owner()) )
												&&
												// and neighbour terrain matches unit category
												((((!ntt.equals(TerrainType.OCEAN)^unit.category().equals(UnitCategory.SEA)) 
														// and unit has no enemy unit nearby, or neighbour square has no unit nearby
														&& (!unitNextToEnemy || !CivLogic.checkSquareNextToEnemyUnit(unit.owner(), nx, ny)) 
														))
														// or the unit category is AIR, so it can ignore enemy units
														|| (unit.category().equals(UnitCategory.AIR)))
												) 
												||
												// or the neighbour square contains a City belonging to the same Civ as the unit
												(
														((Entity)unit).gamesave().hasTerrainImprovement(nx, ny, false, TerrainImprovementType.CITY) 
														&&
														((Entity)unit).gamesave().owner(nx,ny).equals(((Entity)unit).gamesave().player())
														)
										) 
										&&
										// neighbour is not an ocean, or at least size 5 or bigger
										(!ntt.equals(TerrainType.OCEAN)^(getContinentOrOceanSizeAt(((Entity)unit).gamesave(),nx,ny)>=5))
								) {
							int movescore = 0;
							if(!roadUnderUnit || !((Entity)unit).gamesave().hasTerrainImprovement(nx, ny, false, TerrainImprovementType.ROAD)) {
								movescore = unit.getType().totalMoves()<=1?3:3*ntt.movementCost(false);
							} else {
								// No code for railroad !
								movescore = 1;
							}
							movescore += Math.abs(gmux2) + Math.abs(gmuy2) +  distanceFromNeighbourToGoTo*4;
							agmuy2 = ((Short)((Entity)unit).getValue("unkown_unitbyte9")&0xFF);
							int var_6 = 0;
							if(agmuy2!=0xFF) {
								var_6 = Math.abs(agmuy2 - nid);
								if(var_6>4) {
									var_6 = var_6 - 8;
								}
								movescore += var_6*var_6;
							}
							if(movescore<bestMoveScore) {
								next = nid;
								bestMoveScore = movescore;
							}
						}
					}
				}
				if(((Short)unit.getValue("unkown_unitbyte9")&0xFF)!=0xFF) {
					if(
							((
									(Short)unit.getValue("unkown_unitbyte9")
									&
									0xFF
									)^4)
									==
									next
							) {
						unit.setPropertyValue("remaining_moves", (Short)(short)0);
					}
				}
				if(next==0) {
					unit.setPropertyValue("gotox", (Short)(short)0xFF);
					unit.setPropertyValue("unknown9", (Short)(short)0xFF);
					next = -1;
				} else {
					unit.setPropertyValue("unknown9", (Short)(short)next);
				}
				return next;
			}
		}
		return -1;
	}

	public static int getContinentOrOceanSizeAt(AbstractGameState gs, int x, int y) {
		TerrainType tt = gs.getTerrain(x, y);
		String s = tt.equals(TerrainType.OCEAN)?"ocean":"continent";
		int id = gs.landMassID(new MapLocation(x,y));
		return (Short)gs.getValue(""+s+""+id+".size");
	}
	public static boolean checkSquareNextToEnemyUnit(Civ civ, int x, int y) {
		if(!((Entity)civ).gamesave().hasTerrainImprovement(x, y, false, TerrainImprovementType.CITY)) {
			return checkSquareNextToEnemyUnitOrCity(civ, x, y);
		} else {
			return false;
		}
	}
	public static boolean checkSquareNextToEnemyUnitOrCity(Civ civ, int x, int y) {
		TerrainType tt = ((Entity)civ).gamesave().getTerrain(x, y);
		for(int var_loop=1;var_loop<9;var_loop++) {
			int nx = CivLogic.alignXinMapRange(x+CivUtils.relCitySquareX3[var_loop]);
			int ny = y+CivUtils.relCitySquareY3[var_loop];
			TerrainType ntt = ((Entity)civ).gamesave().getTerrain(nx, ny);
			if(!(tt.equals(TerrainType.OCEAN)^ntt.equals(TerrainType.OCEAN))
					&& CivLogic.whatCivOccupies(((Entity)civ).gamesave(), nx, ny)!=null
					&& !CivLogic.whatCivOccupies(((Entity)civ).gamesave(), nx, ny).equals(civ)){
				if(civ.equals(((Entity)civ).gamesave().player())) {
					AbstractSVEUnit u = (AbstractSVEUnit) CivLogic.getUnitAt(((Entity)civ).gamesave(), nx, ny);
					if(u!=null) {
						if(!((Entity)civ).gamesave().player().canSee(u)) {
							((Entity)civ).gamesave().setVisibility(((Entity)civ).gamesave().player(), nx, ny, 1, 1, VisibilityType.VISIBLE);
							// Skipped: redraw Map sqaure terain and units at nx,ny
						}
						if(((Entity)civ).gamesave().hasTerrainImprovement(nx, ny, false, TerrainImprovementType.CITY)) {
							City city = ((Entity)civ).gamesave().getCityAt(nx, ny);
							city.visibleSize(city.actualSize());
							((Entity)civ).gamesave().actualizeVisibleImprovements(nx,  ny,  1,  1);
							((Entity)civ).gamesave().setVisibility(((Entity)civ).gamesave().player(), nx, ny, 1, 1, VisibilityType.VISIBLE);
							// Skipped: redraw Map sqaure terain and units at nx,ny
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	public static boolean hasReachableGoto(AbstractSVEUnit _SVEUnit) {
		ByteBuffer bb = (ByteBuffer)((Entity)_SVEUnit).gamesave().getValue("land_pathfind");
		byte[] pathfind = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());

		int x = _SVEUnit.getLocation().x();
		int y = _SVEUnit.getLocation().y();

		int destx = _SVEUnit.getDestination().x();
		int desty = _SVEUnit.getDestination().y();

		// seg016_628
		int ntid = findClosestConnected4x4Tile(((Entity)_SVEUnit).gamesave(), x, y, !_SVEUnit.category().equals(UnitCategory.SEA));
		if(ntid != -1) {
			x = (x/4)+CivUtils.relCitySquareX3[ntid];
			y = (y/4)+CivUtils.relCitySquareY3[ntid];

			int ntid2 = findClosestConnected4x4Tile(((Entity)_SVEUnit).gamesave(), _SVEUnit.getDestination().x(), _SVEUnit.getDestination().y(), !_SVEUnit.category().equals(UnitCategory.SEA));
			int[] pathFind3 = new int[260];

			int var_2 = 0;
			int var_4 = 0;
			int[] x99 = new int[99];
			int[] y99 = new int[99];
			x99[0] = _SVEUnit.getDestination().x()/4+CivUtils.relCitySquareX3[ntid2];
			y99[0] = _SVEUnit.getDestination().y()/4+CivUtils.relCitySquareY3[ntid2];
			var_2++;

			pathFind3[x99[0]*13+y99[0]] = 1;

			int varc = 0;
			int var_16 = (_SVEUnit.category().equals(UnitCategory.SEA)?1:0);

			int var_A = 0; // ???
			do {
				int var_x99 = x99[var_4];
				int var_y99 = y99[var_4];
				if(var_x99!=x || var_y99!=y) {
					var_A = pathFind3[var_y99+13*var_x99];
					var_4++;
					int var_10 = pathfind[var_y99+13*var_x99+(var_16*0x40)];
					int var_2E = 1;
					do {
						if((var_10&(1<<(var_2E-1)))!=0) {
							int var_30 = var_x99+CivUtils.relCitySquareX3[var_2E];
							if(var_30==20) {
								var_30=0;
							}
							if(var_30==-1) {
								var_30 = 19;
							}
							int var_34 = var_y99 + CivUtils.relCitySquareY3[var_2E];
							if(pathFind3[var_30*13+var_34]==0) {
								pathFind3[var_30*13+var_34] = var_A;
								x99[var_2] = var_30;
								y99[var_2] = var_34;
								var_2++;
							}
						}
						var_2E++;
					} while(var_2E<=8);
				} else {
					varc = 1;
					break;
				}
			} while(var_4!=var_2 && varc==0);
			destx = -1;
			if(varc!=0) {
				int var_8 = 99;
				int var_E = -1;
				int var_10 = pathfind[x+13*y+((_SVEUnit.category().equals(UnitCategory.SEA)?1:0)*0x40)];
				int var_2E = 1;
				while(var_2E<=8) {

					if((var_10&(1<<(var_2E-1)))!=0) {
						int var_30 = x+CivUtils.relCitySquareX3[var_2E];
						if(var_30==20) {
							var_30=0;
						}
						if(var_30==-1) {
							var_30 = 19;
						}
						int var_34 = y + CivUtils.relCitySquareY3[var_2E];
						int var_6 = pathFind3[var_30*13+var_34];
						if(var_6!=0) {
							if(var_6>=var_8) {
								if(var_6==var_8) {
									int var_12 = CivLogic.distance(_SVEUnit.getDestination().x(), var_30*4+1, _SVEUnit.getDestination().y(), var_34*4+1);
									if(var_12<var_A) {
										var_E = var_2E;
										var_A = var_12;
									}
								}
							} else { // var_6 < var_8
								var_8 = var_6;
								var_E = var_2E;
								var_A = CivLogic.distance(_SVEUnit.getDestination().x(), var_30*4+1, _SVEUnit.getDestination().y(), var_34*4+1);
							}
						}
					}
					var_2E++;
				}
				// seg016_977
				if(var_E != -1) {
					destx = CivLogic.alignXinMapRange(4*(x+CivUtils.relCitySquareX3[var_E])+1);
					desty = (4*(y+CivUtils.relCitySquareY3[var_E])+1);

					if(var_16 != (((Entity)_SVEUnit).gamesave().getTerrain(destx, desty).equals(TerrainType.OCEAN)?1:0)) {
						destx++;
						if(var_16 != (((Entity)_SVEUnit).gamesave().getTerrain(destx, desty).equals(TerrainType.OCEAN)?1:0)) {
							desty++;
						}
					}
				}
			}
			if(destx==-1) { 
				destx = _SVEUnit.getDestination().x();
				desty = _SVEUnit.getDestination().y();
			}
			return (varc==1);
		} else {
			destx = _SVEUnit.getDestination().x();
			desty = _SVEUnit.getDestination().y();
			return false; // ????
		}
	}

	public static int findClosestConnected4x4Tile(AbstractGameState gs, int x, int y, boolean land) {
		int tileX = x/4;
		int tileY = y/4;
		int bestNeighbour = -1;
		ByteBuffer bb = (ByteBuffer)gs.getValue("land_pathfind");
		byte[] pathfind = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
		if(pathfind[13*tileX+tileY+(land?0:0x40)]!=0) {
			bestNeighbour = 0;
		}
		if(bestNeighbour==-1) {
			int bestDist = 99;
			int nid = 1;
			while(nid<=8) {
				int ntileX = tileX+CivUtils.relCitySquareX3[nid];
				int ntileY = tileY+CivUtils.relCitySquareY3[nid];

				if( (!land && pathfind[13*ntileX+ntileY+0x40]!=0)
						|| (land && pathfind[13*ntileX+ntileY]!=0)) {
					int currentDist = CivLogic.distanceSub(x-4*ntileX-1, y-4*ntileY-1);
					if(currentDist<bestDist) {
						int nx = 4*ntileX;
						int ny = 4*ntileY;
						if(!(land^gs.getTerrain(nx, ny).equals(TerrainType.OCEAN))) {
							nx++;
							if(!(land^gs.getTerrain(nx, ny).equals(TerrainType.OCEAN))) {
								ny++;
								if(!(land^gs.getTerrain(nx, ny).equals(TerrainType.OCEAN))) {
									nx--;
									if(!(land^gs.getTerrain(nx, ny).equals(TerrainType.OCEAN))) {
										continue;
									}
								}
							}
						}
						if(findPathLength(gs, nx, ny, x, y, land, 18)!=-1) {
							bestDist = currentDist;
							bestNeighbour = nid;
						}
					}
				}
				nid++;
			}
		}
		// Normally, CIV returns 0-false or 1-true, and if true
		// it stores the next tile X and Y in global vars
		// dseg_6512 and dseg_6514 (destX and destY)
		// too cumbersome here, we simply return the resulting
		// neighbour tile ID, and -1 if there is none...
		return bestNeighbour; 
	}

	public static void addReplayEntry(byte[] data, int type, int blen, int b0, int b1, int b2, int b3) {
		int currentSize = Utils.shortLE(data[0], data[1]);
		if(currentSize+blen+2<=data.length-2) {

		}
	}

	public static Civ whatCivOccupies(AbstractGameState gs, int x, int y) {
		if(gs.isOccupied(x, y)) {
			return gs.owner(x, y);
		} else {
			return null;
		}
	}

	public static AbstractSVEUnit createUnit(Civ c, UnitType type, int x, int y) {
		int var_unitID = 0;
		SVECiv civ = (SVECiv)c;

		while(var_unitID<127) {
			if(!civ.unit(var_unitID).exists()) {
				AbstractSVEUnit sveUnit = (AbstractSVEUnit) civ.unit(var_unitID);

				sveUnit.setPropertyValue("position_x", (short)-1);
				sveUnit.setPropertyValue("next_unit_in_stack", (short)-1);

				CivLogic.assignSquareToCiv(civ, x, y);


				AbstractGameState gs = ((Entity)civ).gamesave();
				gs.setVisibility(civ, x, y, 1, 1, VisibilityType.VISIBLE);
				sveUnit.setPropertyValue("status_flag", 0);
				sveUnit.setPropertyValue("position_x", x);
				sveUnit.setPropertyValue("position_y", y);
				sveUnit.setPropertyValue("type", ((SVEUnitType) type).getID());

				CivLogic.putUnitAt(sveUnit, x, y);

				sveUnit.setPropertyValue("visible_by_flag", (1<<civ.getID()));
				sveUnit.setPropertyValue("gotox", -1);
				sveUnit.setPropertyValue("goto_next_direction", -1);
				sveUnit.setRemaining_moves(type.totalMoves()); // NOT FOLLOWING ORIGINAL CIV LOGIC!
				sveUnit.setPropertyValue("special_moves", (type.turnsOutdoors()==0?0:type.turnsOutdoors()-1));
				int nearestCityID = CivLogic.findNearestCityID(gs, x, y);
				if(gs.city(nearestCityID).owner().equals(civ)) {
					sveUnit.setPropertyValue("home_city_id", nearestCityID);
				} else {
					sveUnit.setPropertyValue("home_city_id", -1);
				}

				int au = (Short)gs.getValue("civ"+civ.getID()+".active_units"+((SVEUnitType) type).getID());
				gs.setPropertyValue("civ"+civ.getID()+".active_units"+((SVEUnitType) type).getID(),au+1);

				//if(dseg_20F4)
				//if(cheat enabled ||
				if(civ.equals(gs.player())||gs.player().canSee(sveUnit)) {
					// TODO: draw map square and units
					//JCivLogic.drawMapTerrainSquare(map, x, nearestCityID, bi, imgx, imgy, showAll, mode, drawLand, drawSpecial, drawRoad, drawImpr, drawPollution, drawHuts, drawShadow, drawCity)
				}

				return sveUnit;
			}
			var_unitID++;
		}

		return null;
	}

	public static int nextCityName(Civ civ) {
		CivDosGameState gs = (CivDosGameState) civ.gamesave();

		int who = (short) ((Entity) civ).getValue("who");
		int cityNameID = -1;
		cityNameID = who - (who<8?1:2);
		if(who == -1 || civ.getID() == 0) { // if Barbarians,
			cityNameID = 14; // directly start in additional city names
		}
		cityNameID = cityNameID<<4; // *16
		int var_cityBaseID = cityNameID;
		boolean var_loopStop = true;

		do {
			var_loopStop = true;
			int _cvid = cityNameID/16;
			String prefix = "civ"+_cvid+".city"+(cityNameID%16)+".";
			byte _x = (Byte)gs.getValue(prefix+"position_X");
			if(_x!=-1) {
				var_loopStop = false;
				cityNameID++;
				if(cityNameID==var_cityBaseID+16) {
					cityNameID = 224;
				}
				if(cityNameID==256) {
					cityNameID=0;
					if( var_cityBaseID == 999 ) {
						var_loopStop = true;
						return -1;
					}
					var_cityBaseID = 999;
				}
			}
		}while(!var_loopStop);

		return cityNameID;
	}

	public static int buildCity(Civ civ, int x, int y, int initSize, int arg1,String name) {
		if(civ!=null) {
			AbstractGameState gs = ((Entity)civ).gamesave();
			if(gs!=null) {
				for(int var_loop=0;var_loop<128;var_loop++) {
					if(!((SVEMapEntity) gs.city(var_loop)).exists()) {

						// routine for next city name extracted to separate
						// function for reusability
						int cityNameID = nextCityName(civ);

						int _cvid = cityNameID/16;
						String prefix0 = "civ"+_cvid+".city"+(cityNameID%16)+".";
						AbstractCity newCity = (AbstractCity) gs.city(var_loop);
						newCity.setPropertyValue("city_name_id", (short)cityNameID);
						gs.setPropertyValue(prefix0+"position_X", (byte)x);
						gs.setPropertyValue(prefix0+"position_Y", (byte)y);

						// Skipped: only ask for city name if Civ is player
						if(name==null) {
							return -1;
						}
						String prefix1 = "civ"+_cvid+".city_name"+(cityNameID%16)+"";
						gs.setPropertyValue(prefix1, name);

						/* TODO: Write Replay Entry */

						// Remove mining and fortress
						gs.removeTerrainImprovement(x, y, true, TerrainImprovementType.FORTRESS);
						gs.removeTerrainImprovement(x, y, true, TerrainImprovementType.MINING);

						// Set terrain improvement flags city and road
						gs.addTerrainImprovement(x, y, true, TerrainImprovementType.CITY);
						gs.addTerrainImprovement(x, y, true, TerrainImprovementType.ROAD);
						//						int imps = gs.getTerrainImprovementsFlag(x, y, true);
						//						imps = ((imps&0b11110000)|0b1001);
						//						gs.setTerrainImprovementFlag(x, y, 1, 1, true, imps);

						TerrainType tt = gs.getTerrain(x, y);
						if(tt.irrigationEffect(false)<-1) {
							gs.addTerrainImprovement(x, y, true, TerrainImprovementType.IRRIGATION);
						}

						newCity.setPropertyValue("status_flag", (byte)0);
						newCity.setPropertyValue("position_x", (byte)x);
						newCity.setPropertyValue("position_y", (byte)y);
						newCity.setPropertyValue("actual_size", (byte)initSize);
						newCity.setPropertyValue("visible_size", (byte)0);
						newCity.setPropertyValue("workers_flag0", (byte)0);
						newCity.setPropertyValue("workers_flag1", (byte)0);
						newCity.setPropertyValue("workers_flag2", (byte)0);
						newCity.setPropertyValue("workers_flag3", (byte)0);
						newCity.setPropertyValue("base_trade", (byte)0);
						newCity.setPropertyValue("food_count", (short)0);
						newCity.setPropertyValue("shields_count", (short)0);

						newCity.setPropertyValue("buildings_flag0", (byte)0);
						newCity.setPropertyValue("buildings_flag1", (byte)0);
						newCity.setPropertyValue("buildings_flag2", (byte)0);
						newCity.setPropertyValue("buildings_flag3", (byte)0);

						newCity.setPropertyValue("current_prod_id", (short)1); // militia
						if(civ.knows(Advance.GUNPOWDER)) { 
							newCity.setPropertyValue("current_prod_id", (short)4); // musketeer
						}
						if(civ.knows(Advance.CONSCRIPTION)) { 
							newCity.setPropertyValue("current_prod_id", (short)5); // riflemen
						}
						newCity.setPropertyValue("unknown_cb27", (byte)0xFF);
						newCity.setPropertyValue("unknown_cb28", (byte)0xFF);

						boolean firstCity = true;
						for(int var_loop2 = 0; var_loop2<128;var_loop2++) {
							if(gs.city(var_loop2).owner().equals(civ) && !newCity.equals(gs.city(var_loop2))) {
								firstCity = false;
							}
						}
						if(firstCity) {
							newCity.add(CityImprovement.PALACE);
						}

						newCity.setPropertyValue("base_trade", (byte)0);
						newCity.setPropertyValue("owning_civ", (byte)((SVECiv) civ).getID());

						newCity.setPropertyValue("trade_city1", (short)0xFF);
						newCity.setPropertyValue("trade_city2", (short)0xFF);
						newCity.setPropertyValue("trade_city3", (short)0xFF);

						newCity.setPropertyValue("workers_flag4", (byte)0); // specialists
						newCity.setPropertyValue("workers_flag5", (byte)0);

						for(int var_loop2=1;var_loop2<=8;var_loop2++) {
							TerrainType nt = gs.getTerrain(x+CivUtils.relCitySquareX3[var_loop2], y+CivUtils.relCitySquareY3[var_loop2]);

							if(nt.equals(TerrainType.OCEAN)) {
								// TODO: Check if neighbour is mega-ocean OR belongs to player
								//newCity.setValue("status_flagbit1", (boolean)true); // coastal city
								newCity.set(CityStatus.COASTAL, true);
							}
							if(nt.equals(TerrainType.RIVER) || nt.equals(TerrainType.MOUNTAINS)) {
								//newCity.setValue("status_flagbit3", (boolean)true); // can build hydro-plant
								newCity.set(CityStatus.HYDRO_AVAILABLE, true);
							}
						}
						// skipped: if first city, ONLINE help is displayed

						// Painting current land values and improvements
						for(int var_loop2=0;var_loop2<45;var_loop2++) {
							int nx = CivLogic.alignXinMapRange(x+CivUtils.relCitySquareX3[var_loop2]);
							int ny = y+CivUtils.relCitySquareY3[var_loop2];
							if(var_loop2>20) {
								int val = gs.getTerrainValue(nx, ny, false);
								if(val>=8) {
									gs.setTerrainValue(nx, ny, false, 8);
								}
							} else {
								gs.setTerrainValue(nx, ny, false, ((SVECiv) civ).getID());
							}
							if(civ.equals(gs.player())
									&& var_loop2<=24
									&& gs.hasTerrainImprovement(nx, ny, false, TerrainImprovementType.CITY)
									&& gs.owner(nx, ny).equals(gs.player())) {
								// update visible improvements from actual improvements
								newCity.setPropertyValue("visible_size", (byte)1);
								gs.actualizeVisibleImprovements(x, y, 1, 1);
								//int imps2 = gs.getMap().getTerrainImprovementsFlag(x, y, true);
								//gs.getMap().setTerrainImprovementFlag(x, y, 1, 1, false, imps2);
							}
						}

						if(civ.equals(gs.player())) {
							// Skipped: if built by player and game settings allow, trigger animation
						} else {
							if(isWonderApplicable(gs.player(),WonderType.APOLLO_PROGRAM)) {
								//								int imps2 = gs.getTerrainImprovementsFlag(x, y, true);
								//								gs.setTerrainImprovementFlag(x, y, 1, 1, false, imps2);
								gs.actualizeVisibleImprovements(x, y, 1, 1);
							}
						}
						return var_loop;
					}
					// Ending loop without returning: no city slot available!

					// Skipped: inform player that the maximum city limit is reached
				}
			}
		}



		return -1;
	}

	public static boolean isWonderApplicable(Civ civ, WonderType w) {
		if(w.getDeprecatingAdvance()!=null) {
			for(int i=0;i<8;i++) {
				if(civ.gamesave().civ(i) != null
						&&
						civ.gamesave().civ(i).knows(w.getDeprecatingAdvance())) {
					return false;
				}
			}
		}
		if(((Entity)civ).gamesave().wonder(w.getId()-1).getHostCity()!=null
				&& ((Entity)civ).gamesave().wonder(w.getId()-1).getHostCity().owner().equals(((Entity)civ).gamesave().player())) {
			return true;
		}
		return false;
	}

	public static int getLandOccupation(Map wm, int x, int y) {
		if(wm.isOccupied(x, y)) {
			return ((SVECiv) wm.owner(x, y)).getID();
		} else {
			return -1;
		}
	}
	public static int distanceSub(int x, int y) {
		return (Math.max(Math.abs(x), Math.abs(y)) + Math.min(Math.abs(x), Math.abs(y))/2);
	}

	public static int distance(int x1, int x2, int y1, int y2) {
		int x = Math.abs(x1-x2);
		if(x>40) x = 80-x; // HARDCODED
		int y = Math.abs(y1-y2);
		return distanceSub(x,y);
	}

	public static int findNearestCityID(AbstractGameState gs, int x, int y) {
		int nearestCity = -1;
		int bestDistance = 999;
		for(City c : gs.getAllCities()) {
			if(c!=null && ((SVEMapEntity) c).exists()) {
				int d = distance(x,c.getLocation().x(),y,c.getLocation().y());
				if(d<bestDistance) {
					bestDistance = d; // MEM:To be stored in memory
					nearestCity = ((SVECity) c).getID();
				}
			}
		}
		return nearestCity;
	}

	public static void assignSquareToCiv(Civ civ, int x, int y) {
		if(civ!=null && ((Entity)civ).gamesave() !=null && ((Entity)civ).gamesave()!=null) {
			Map map = ((Entity)civ).gamesave();
			map.setOwner(x, y, civ);
		}
	}

	// TODO: start
	public static int dseg_2496_cityViewActiveTab;
	static int dseg_2494;
	static int dseg_F2E2;
	static int dseg_EA62;

	static int dseg_2F4E;
	static int dseg_C108_openCityScreenFlag;

	static int currentProcessingCity_dseg_64C2;
	static int dseg_6AB0_distanceToCapital;
	static int[] dseg = new int[0xFFFF];
	static int cityOwner_dseg_64CA;
	static int neighbourSquareCount_dseg_64C4 = 0;
	static int pollutionFactor_dseg_C7A2;
	static int citySpecialistCount_dseg_F7D8;

	static int dseg_64C8_away_unit_counter;
	
	static int dseg_EDD6;
	static int dseg_EDD8_unk27_28_counter;
	static int dseg_E216_unitShieldMaintenanceCost;
	static int dseg_F2E6_settler_counter;
	static int dseg_7068_playerTrespassingAIcityFlag;
	static int cityFoodProd_dseg_705A;
	static int cityShieldProd_dseg_705C;
	static int cityTradeProd_dseg_705E;
	static int dseg_E200_corruption;
	static int cityLuxuryProd_dseg_7060;
	static int dseg_6C18_cityPowerType;


	static void drawResourcesOnCityMapSquare(AbstractGameState gs, BufferedImage bi, int arg_cityID, int var_neighbourID,int arg_display) {
		City city = gs.city(arg_cityID);
		Civ cityOwner = city.owner();
		Graphics2D gfx = bi.createGraphics();
		int dx = CivUtils.relCityX[var_neighbourID];
		int dy = CivUtils.relCityY[var_neighbourID];
		int var_neighbourX = city.getLocation().x() + dx;
		int var_neighbourY = city.getLocation().y() + dy;
		int var_iconSpace = 8;
		int var_iconX;
		int var_iconY;
		int var_resType;
		int var_squareScore = 0;
		//System.out.println("== drawing resources: cityID: "+arg_cityID+" ["+city.getName()+"] at ("+city.getPosition_x()+","+city.getPosition_y()+"); neighbourID = "+var_neighbourID+" relative:("+dx+","+dy+"), absolute:("+var_neighbourX+","+var_neighbourY+")");
		if(arg_display==1) {
			ImageUtils.drawMapTerrainSquare(gs, var_neighbourX, var_neighbourY, bi, 80+(5+dx)*16,8+(3+dy)*16, gs.getRandomSeed(), 1, true,true,true,true,true,true,true,true);
			var_squareScore = 0;
			var_resType = 0;
			while(var_resType<3) {
				//System.out.println("   resource ID: "+var_resType);
				int _rs = computeMapSquareResources(gs, var_neighbourX, var_neighbourY, var_resType);
				//System.out.println("        amount: "+_rs);

				var_squareScore += _rs;
				var_resType++;
			}
			//System.out.println(" total square resources:"+var_squareScore);
			if(var_squareScore>4) {
				if(var_squareScore>6) {
					var_iconSpace = 3;
				} else {
					var_iconSpace = 5;
				}
			} else {
				var_iconSpace = 8;
			}
		}

		var_iconY = 0;
		var_iconX = 0;
		var_resType = 0;

		String debug = "DEBUG: central square resources: ";
		while(var_resType<3) {
			if(dx==0 && dy ==0) {
				String cp = "checkpoint";
			}
			int var_resCount = computeMapSquareResources(gs, var_neighbourX, var_neighbourY, var_resType);

			if(dx==0 && dy ==0) {
				debug += var_resCount+" "+(var_resType==0?"food, ":var_resType==1?"shields, ":"trade");
			}
			switch(var_resType) {
			case 0:
				cityFoodProd_dseg_705A += var_resCount;
				break;
			case 1:
				cityShieldProd_dseg_705C += var_resCount;
				break;
			case 2:
				cityTradeProd_dseg_705E += var_resCount;
				break;
			default:
				break;
			}
			//dseg_705A_resourceCounters[var_resType] += var_resCount;

			if(arg_display == 1) {
				while(var_resCount>0) {
					int rimgx = CivUtils.relCityX[var_neighbourID]*16+var_iconX+161; 
					int rimgy = CivUtils.relCityY[var_neighbourID]*16+var_iconY+57;

					String restype = var_resType==0?"food":var_resType==1?"shield":var_resType==2?"trade":null;
					if(restype!=null) {
						BufferedImage resicon = ResourceManager.getSprite("city.resource."+restype+".");
						gfx.drawImage(resicon, rimgx, rimgy, null);
					}

					if(var_iconX<8) {
						var_iconX += var_iconSpace;
					} else {
						var_iconX = 0;
						var_iconY += 8;
					}
					var_resCount--;
				}
			}
			var_resType++;
		}
		if(dx==0 && dy==0) System.out.println(debug);

		if(var_squareScore==0) {
			if(arg_display==1) {
				// Draw sad face icon (13) at relative offset (61,165)
			}
		}

		dseg_2494 = 0;
		gfx.dispose();
	}

	public static int computeMapSquareResources(AbstractGameState gs, int x, int y, int resType) {
		if(CivLogic.isInMap(x, y)) {
			// resType: 0-food, 1-shields, 2-trade
			TerrainType tt = gs.getTerrain(x, y);
			boolean special = CivLogic.isSpecialResource(x, y, gs.getRandomSeed());
			int var_prod = resType==0?tt.food(special):
				resType==1?tt.shields(special):
					resType==2?tt.trade(special):0;
					//tt..getAttributes(special)[2+resType];
					boolean showAll = true;
					Map wm = gs;
					//int var_impr = gs.getTerrainImprovementsFlag(x, y, showAll);
					// if DebugSwitch contains 0x10 skip below...
					//		int ax;
					//		if(tt.above(TerrainType.GRASSLANDS)) {
					//			var_impr = 2;
					//		} else {
					//			var_impr = 4;
					//		}
					//		if(tt.equals(TerrainType.PLAINS)) {
					//			var_impr |= 0b1000;
					//		}

					if(!tt.equals(TerrainType.OCEAN)) {
						if(resType == 0) { // food
							if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.IRRIGATION)) {
								var_prod += (-1 - tt.irrigationEffect(special));
							}
						}
						if(resType == 1) { // shields
							if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.MINING)) {
								var_prod += (-1 - tt.miningEffect(special));
							}
						}
						if(resType == 2) { // trade
							if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.ROAD)) {
								if(tt.equals(TerrainType.DESERT)
										|| tt.equals(TerrainType.GRASSLANDS)
										|| tt.equals(TerrainType.PLAINS)) {
									var_prod ++;
								}
							}
						}
					}
					if(resType==1) {
						if(tt.equals(TerrainType.GRASSLANDS)||tt.equals(TerrainType.RIVER)) {
							if(((7*x+11*y) & 0b10) != 0) { // special formula
								var_prod = 0;
							}
						}
					}
					if(var_prod!=0) {
						if(resType==2) {
							int host = gs.wonder(WonderType.COLOSSUS.getId()-1).getHostCityID();
							if(host==currentProcessingCity_dseg_64C2) {
								var_prod++;
							}
						}
					}
					if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.RAILROAD)) {
						var_prod += (var_prod>>1); // adds 50% of everything
					}
					if(var_prod>2) {
						if(!gs.city(currentProcessingCity_dseg_64C2).has(CityStatus.RAPTURE_2)) { // 
							if((Short)((Entity) gs.civ(cityOwner_dseg_64CA)).getValue("government")<=1) {
								var_prod--;
							}
							if(dseg_2494!=0) {
								dseg_F2E2 -= 2;
							}
						}
					}
					if(var_prod!=0) {
						if(resType==2) {
							if(dseg_2494!=0) {
								dseg_EA62++;
							}
							int _ax;
							if(gs.city(currentProcessingCity_dseg_64C2).has(CityStatus.RAPTURE_2)) {
								_ax = 2;
							} else {
								_ax = 4;
							}
							if(_ax<=(Short)((Entity) gs.civ(cityOwner_dseg_64CA)).getValue("government")) {
								var_prod++;
							}
						}
					}
					if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.POLLUTION)) {
						var_prod = (var_prod+1)>>1; // pollution divides by 2
					}
					return Math.max(0, var_prod);
		}
		return 0;
	}

	public static int getHostCityOfWonder(AbstractGameState gs, int wonderID) {
		return gs.wonder(wonderID).getHostCityID();
	}

	public static String populationString(int pop) {
		String s = "";
		if(pop>=100) {
			s += pop/100;
			s += ",";
			if(pop%100<10) {
				s += "0";
			}
		}
		s += pop%100;
		s += "0,000";
		return s;
	}

	public static boolean isSpecialResource(int x, int y, int TMW) {
		//		if(x==0xA && y==0x12) {
		//			int g = 0;
		//		}
		//
		//		CivRandom cr  = new CivRandom(TMW);
		//		cr.ds5BDC = (short) 0xBABE;
		//		int base = y*TMW+x*0xBABE;
		//		int n = cr.next(y*TMW+x*0xBABE);
		//		//int n = cr.next(y+x);
		//		
		//		int xn = (n^0xCAFE)&0xFFFF;
		//		int xnm = xn%5;
		//		
		//		return (xnm==0);

		return (y>1 && y<48 &&
				(((x&3)<<2) + (y&3)) == (((((x>>2)*13) + ((y>>2)*11)) + TMW ) & 0xF ));
	}
	public static boolean isHut(int x, int y, int TMW, boolean activeOnly, AbstractGameState gs) {
		if(activeOnly && gs!=null) {
			return (y>1 && y<48 &&
					!gs.getTerrain(x, y).equals(TerrainType.OCEAN) &&
					!gs.civ(0).canSee(new MapLocation(x, y)) &&
					(((x&3)<<2) + (y&3)) ==  (((((x>>2)*13) + ((y>>2)*11)) + TMW + 8) & 0x1F ));
		} else {
			return (y>1 && y<48 &&
					(((x&3)<<2) + (y&3)) == (((((x>>2)*13) + ((y>>2)*11)) + TMW + 8) & 0x1F ));
		}
	}

	public static int rangeBound(int val, int min, int max) {
		return Math.min(Math.max(val, min), max);
	}

	public static void drawUnits(AbstractGameState gs, int x, int y, BufferedImage bi, BufferedImage gotobi, int imgx, int imgy, int mode, boolean showGoto) {
		if(!gs.getUnitsAt(x, y).isEmpty()) {
			Unit _SVEUnit = gs.getUnitsAt(x, y).iterator().next();
			_SVEUnit = CivLogic.determineUnitToDraw(gs, x, y, (SVEUnit) _SVEUnit);
			drawUnit((SVEUnit) _SVEUnit,bi,imgx,imgy,mode);

			if(showGoto && gotobi!=null) {
				// Draw GoTo arrows! Yeah!
				Collection<? extends Unit> units = gs.getUnitsAt(x, y);
				for(Unit unit : units) {
					// TODO: deal with "world is round"
					if(unit.getDestination()!=null) {
						Point2D start = new Point2D.Double(imgx+8, imgy+8);
						MapLocation dest = unit.getDestination();
						int destimgx = imgx + (dest.x()-x)*16;
						int destimgy = imgy + (dest.y()-y)*16;
						Point2D end = new Point2D.Double(destimgx+8, destimgy+8);
						if(Math.abs(dest.x()-x)>40) {
							// Take shortest route, by the other side:
							// Need to draw 2 partial arrows
							if(x>=40) { // start is in the East, dest in the West
								int fakedestx = dest.x() + 80;
								int fakedestimgx = imgx + 16*fakedestx;
								Point2D fakeend = new Point2D.Double(fakedestimgx+8, destimgy+8);
								ImageUtils.drawArrow(start, fakeend, gotobi, ImageUtils.civColors[unit.owner().getID()][0]);

								//int fakestartx = x - 80;
								int fakestartimgx = imgx - 80*16;
								Point2D fakestart = new Point2D.Double(fakestartimgx+8, imgy+8);
								ImageUtils.drawArrow(fakestart, end, gotobi, ImageUtils.civColors[unit.owner().getID()][0]);
							} else { // dest is in the East, start in the West
								int fakedestx = dest.x() - 80;
								int fakedestimgx = imgx + 16*fakedestx;
								Point2D fakeend = new Point2D.Double(fakedestimgx+8, destimgy+8);
								ImageUtils.drawArrow(start, fakeend, gotobi, ImageUtils.civColors[unit.owner().getID()][0]);

								//int fakestartx = x - 80;
								int fakestartimgx = imgx + 80*16;
								Point2D fakestart = new Point2D.Double(fakestartimgx+8, imgy+8);
								ImageUtils.drawArrow(fakestart, end, gotobi, ImageUtils.civColors[unit.owner().getID()][0]);
							}
						} else {
							ImageUtils.drawArrow(start, end, gotobi, ImageUtils.civColors[unit.owner().getID()][0]);
						}
					}
				}
			}
		}
	}
	public static void drawUnit(SVEUnit _SVEUnit, BufferedImage bi, int imgx, int imgy, int mode) {
		drawUnit(_SVEUnit, bi, imgx, imgy, mode, true);
	}
	public static void drawUnit(SVEUnit _SVEUnit, BufferedImage bi, int imgx, int imgy, int mode, boolean force) {
		Graphics2D gfx = bi.createGraphics();
		AbstractGameState gs = ((Entity)_SVEUnit).gamesave();
		Map map = gs;
		int x = _SVEUnit.getLocation().x();
		int y = _SVEUnit.getLocation().y();
		boolean modeT = false;
		if(mode==0) {
			// seg012_EA4:
			// skipped: Theoretical "imgx" is computed as "imgx = (map_viewport_x - x) * 16 + 80"
			// skipped: Theoretical "imgy" is computed as "imgy = (map_viewport_y - y) * 16 + 8"
			// skipped: target imgx and imgy are tested to be within the "MAP" window of CIV screen:
			//     80 <= imgx < 320
			//      8 <= imgx < 192

			TerrainType tt = map.getTerrain(x, y);
			if(!tt.equals(TerrainType.OCEAN)
					|| !_SVEUnit.getStatus().equals(UnitStatus.SENTRY)
					|| _SVEUnit.getType().category().equals(UnitCategory.SEA)
					|| force) {
				BufferedImage unitIcon = ImageUtils.getCivUnitIcon(_SVEUnit);
				if(_SVEUnit.hasNextInStack()) {
					gfx.drawImage(unitIcon,imgx+1,imgy+1,null);
				}
				gfx.drawImage(unitIcon,imgx,imgy,null);
				// Status overlays
				char status = 0;
				if(_SVEUnit.getStatus().equals(UnitStatus.FORTIFIED)/*||u.hasStatus(UnitStatus.JUST_FORTIFIED)*/) {
					gfx.drawImage(ResourceManager.getSprite("terrain.overlay.citywalls."), imgx,imgy,null);
				}
				if(_SVEUnit.getStatus().equals(UnitStatus.ROADBUILDING))			status = 'R';
				if(_SVEUnit.getStatus().equals(UnitStatus.IRRIGATING))			status = 'I';
				if(_SVEUnit.getStatus().equals(UnitStatus.MINING))				status = 'M';
				if(_SVEUnit.getStatus().equals(UnitStatus.BUILDING_FORTRESS))	status = 'F';
				if(_SVEUnit.getStatus().equals(UnitStatus.JUST_FORTIFIED))		status = 'F';
				if(_SVEUnit.getStatus().equals(UnitStatus.DEPOLLUTING))			status = 'P';
				if(_SVEUnit.getDestination()!=null)					status = 'G';
				if(status!=0) {
					CivDosRenderer.drawString(""+status, gfx, 0, Palette.defaultColors.get(0), imgx+4, imgy+7+8+1);
					Color lightCol = Palette.defaultColors.get(((SVECiv) _SVEUnit.owner()).getID()==1?9:15);
					CivDosRenderer.drawString(""+status, gfx, 0, lightCol, imgx+4, imgy+7+8);
					gfx.setColor(Palette.defaultColors.get(7)); // very surprised by this gray frame, never noticed...
					gfx.drawRect(imgx-1, imgy-1, 15, 15);
				}
			}
		} else {
			Color backCol = Palette.defaultColors.get(0);
			Color lightCol = ImageUtils.civColors[((SVECiv) _SVEUnit.owner()).getID()][0];
			if(_SVEUnit.typeID()==0) {
				backCol = Palette.defaultColors.get(6);
			}
			gfx.setColor(backCol);
			gfx.fillRect(imgx*4+1, imgy*4+1, 3, 3);
			gfx.setColor(lightCol);
			gfx.fillRect(imgx*4, imgy*4, 3, 3);
		}
	}

	public static void drawCity(City city /*GameSave gs,*//*, int x, int y, */, BufferedImage bi, BufferedImage namebi, int imgx, int imgy, boolean showAll, boolean showName) {
		//if(gs.getCityAt(x, y)!=null &&
		//		(gs.getCityAt(x, y).owner().equals(gs.getPlayerCiv()) ||
		//				gs.getCityAt(x, y).getVisibleSize()>0) || showAll) {
		Color lightCol = ImageUtils.civColors[((SVECiv) city.owner()).getID()][0];
		Color darkCol = ImageUtils.civColors[((SVECiv) city.owner()).getID()][1];
		boolean inRiot = city.has(CityStatus.RIOT);
		int size = city.getVisibleSize();
		if(city.owner().equals(((Entity)city).gamesave().player()) || showAll) {
			size = city.actualSize();
		}
		boolean hasUnits = city.hasUnits() || (((Short)((Entity) city).getValue("unknown_cb27"))&0xFF)!=0xFF;
		boolean hasWalls = city.has(CityImprovement.CITY_WALLS);
		String name = (showName?city.getName():null);

		CivDosRenderer.drawCity(bi,namebi,imgx,imgy,lightCol, darkCol, inRiot,hasUnits,hasWalls,size,name);
		//}
	}

	public static void drawMapTerrainSquare(Map wm, /*GameSave gs, */int x, int y, BufferedImage bi, int imgx, int imgy, boolean showAll, int mode,
			boolean drawLand, boolean drawSpecial, boolean drawRoad, boolean drawImpr, boolean drawPollution, boolean drawHuts, 
			boolean drawShadow, boolean drawCity) {
		/*WorldMap map = gs.getMap();*/
		//GameSave gs = map.getGameSave();

		Graphics2D gfx = bi.createGraphics();
		boolean modeT = false;
		if(mode==0) {

			// seg012_427:
			// skipped: Theoretical "imgx" is computed as "imgx = (map_viewport_x - x) * 16 + 80"
			// skipped: Theoretical "imgy" is computed as "imgy = (map_viewport_y - y) * 16 + 8"
			// skipped: target imgx and imgy are tested to be within the "MAP" window of CIV screen:
			//     80 <= imgx < 320
			//      8 <= imgx < 192


			TerrainType var_terrainType = wm.getTerrain(x, y);
			//int var_impr = wm.getTerrainImprovementsFlag(x, y, showAll);

			if(var_terrainType.equals(TerrainType.OCEAN) && drawLand) {
				// skipped: if(EGA or VGA); only dealing with VGA 256 color
				int var_neighboursFlag = 0;

				// seg012_540
				for(int var_neighbourID = 1; var_neighbourID<9; var_neighbourID++) {
					var_neighboursFlag = (var_neighboursFlag >> 1);

					int nx = alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
					int ny = y+CivUtils.relCitySquareY3[var_neighbourID];

					if(isInMap(nx,ny)) {
						TerrainType var_neighbourType = wm.getTerrain(nx,ny);
						if(!var_neighbourType.equals(TerrainType.OCEAN)) {
							var_neighboursFlag |= 0b10000000;
						}
					}
				}

				int varCitySize = var_neighboursFlag;

				// [1] The block below shifts the 2 highest bits to the 2 lowest positions			
				int ax = var_neighboursFlag;
				ax = (ax>>6);
				ax = (ax&3);
				int cx = var_neighboursFlag;
				cx = (cx<<2);
				cx = cx+ax;
				var_neighboursFlag = cx;
				// end of [1]

				for(int var_neighbourID = 0; var_neighbourID<4; var_neighbourID++) {
					int coast_special_id = 0;
					int _x = 0;
					int _y = 0;
					if(var_neighbourID<2) {
						int si = var_neighboursFlag;
						cx = var_neighbourID;
						cx = (cx<<1);  // cx = 2 * var_neighbourID
						si = (si>>cx); // si = (var_neighboursFlag >> (2 * var_neighbourID) )
						si = (si&7);   // si = (var_neighboursFlag >> (2 * var_neighbourID) ) & 0b111
						si = (si<<3);  // si = ( (var_neighboursFlag >> (2 * var_neighbourID) ) & 0b111 ) << 3

						int bx = var_neighbourID*2;

						coast_special_id = (bx+si)/2;

						_y = imgy;
						_x = imgx + (var_neighbourID&1)*8;
					} else {
						int si = var_neighboursFlag;
						cx = var_neighbourID;
						cx = (cx<<1);  // cx = 2 * var_neighbourID
						si = (si>>cx); // si = (var_neighboursFlag >> (2 * var_neighbourID) )
						si = (si&7);   // si = (var_neighboursFlag >> (2 * var_neighbourID) ) & 0b111
						si = (si<<3);  // si = ( (var_neighboursFlag >> (2 * var_neighbourID) ) & 0b111 ) << 3

						int bx = var_neighbourID*2;

						coast_special_id = (bx+si)/2;

						_y = imgy +8;
						_x = imgx - (var_neighbourID&1)*8 +8;

					}
					BufferedImage coastelem = ResourceManager.getSprite("terrain.overlay.coast_special."+coast_special_id+".");
					gfx.drawImage(coastelem, _x, _y, null);
				}

				BufferedImage coastelem = null;
				if(varCitySize == 0x1C) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0x1C.");
				if(varCitySize == 0xC1) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0xC1.");
				if(varCitySize == 0x07) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0x07.");
				if(varCitySize == 0x70) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0x70.");
				if(varCitySize == 0x8F) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0x8F.");
				if(varCitySize == 0xF8) coastelem = ResourceManager.getSprite("terrain.overlay.coast_special.0xF8.");
				if(coastelem!=null) gfx.drawImage(coastelem, imgx, imgy, null);


				for(int var_neighbourID = 1; var_neighbourID<9; var_neighbourID+=2) {
					int nx = alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
					int ny = y+CivUtils.relCitySquareY3[var_neighbourID];

					if(isInMap(nx,ny)) {
						TerrainType var_neighbourType = wm.getTerrain(nx,ny);
						if(var_neighbourType.equals(TerrainType.RIVER)) {
							BufferedImage rivermouth = ResourceManager.getSprite("terrain.overlay.river."+(32 + var_neighbourID/2)+".");
							gfx.drawImage(rivermouth, imgx, imgy, null);
						}
					}
				}
			}

			if(!var_terrainType.equals(TerrainType.OCEAN) && drawLand) {
				BufferedImage landbase = ResourceManager.getSprite("terrain.base.land.1.");
				gfx.drawImage(landbase, imgx, imgy, null);
			}

			if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.IRRIGATION)
					&& !var_terrainType.equals(TerrainType.OCEAN)
					&& !modeT
					&& !wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.CITY)
					&& drawImpr) {
				BufferedImage irrig = ResourceManager.getSprite("terrain.overlay.irrigation.");
				gfx.drawImage(irrig, imgx, imgy, null);
			}

			if(var_terrainType.equals(TerrainType.RIVER) && drawLand) {
				int var_neighboursFlag = 0;
				int var_neighbourID = 1;
				while(var_neighbourID<9) {
					var_neighboursFlag = var_neighboursFlag >> 1;
				int nx = alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
				int ny = y+CivUtils.relCitySquareY3[var_neighbourID];
				if(isInMap(nx,ny)) {
					TerrainType var_neighbourType = wm.getTerrain(nx,ny);
					if(var_neighbourType.equals(TerrainType.RIVER)
							|| var_neighbourType.equals(TerrainType.OCEAN)) {
						var_neighboursFlag |= 0b1000; // 0x8
					}
				}
				var_neighbourID += 2;
				}

				if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.SPECIAL_RIVER)) {			
					var_neighboursFlag += (0x10 - 1);
				}

				BufferedImage river = ResourceManager.getSprite("terrain.overlay.river."+(var_neighboursFlag+1)+".");
				gfx.drawImage(river, imgx, imgy, null);
			}

			if(!var_terrainType.equals(TerrainType.OCEAN)
					&&!var_terrainType.equals(TerrainType.RIVER)
					&& drawLand) {
				int var_neighboursFlag = 0;
				int var_neighbourID = 1;
				while(var_neighbourID<9) {
					var_neighboursFlag = var_neighboursFlag >> 1;
				int nx = alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
				int ny = y+CivUtils.relCitySquareY3[var_neighbourID];
				if(isInMap(nx,ny)) {
					TerrainType var_neighbourType = wm.getTerrain(nx,ny);
					if(var_neighbourType.equals(var_terrainType)) {
						var_neighboursFlag |= 0b1000; // 0x8
					}
				}
				var_neighbourID += 2;
				}
				String tcode = var_terrainType.getCode();
				BufferedImage overlay = ResourceManager.getSprite("terrain.overlay."+tcode+"."+(var_neighboursFlag+1)+".");
				gfx.drawImage(overlay, imgx, imgy, null);
			}

			if(var_terrainType.equals(TerrainType.GRASSLANDS) && drawLand) {
				if(((7*x + 11*y)&0x2) == 0) {
					BufferedImage overlay = ResourceManager.getSprite("city.resource.grassland.");
					gfx.drawImage(overlay, imgx+4, imgy+4, null);
				}
			}

			if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.POLLUTION) && drawPollution) {
				BufferedImage overlay = ResourceManager.getSprite("terrain.overlay.pollution.");
				gfx.drawImage(overlay, imgx, imgy, null);
			}

			if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.ROAD) && drawRoad) {
				int var_20 = 6;
				if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.RAILROAD)) {
					var_20 = 0;
				}
				for(int var_neighbourID=1;var_neighbourID<=8;var_neighbourID++) {
					int nx = alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
					int ny = y+CivUtils.relCitySquareY3[var_neighbourID];
					if(wm.hasTerrainImprovement(nx, ny, showAll, TerrainImprovementType.ROAD)) {
						var_20 = -1;
						if((wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.RAILROAD)||
								wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.CITY))
								&&
								(wm.hasTerrainImprovement(nx, ny, showAll, TerrainImprovementType.RAILROAD)||
										wm.hasTerrainImprovement(nx, ny, showAll, TerrainImprovementType.CITY))) {
							BufferedImage overlay = ResourceManager.getSprite("terrain.overlay.railroad."+var_neighbourID+".");
							gfx.drawImage(overlay, imgx, imgy, null);
						} else {
							BufferedImage overlay = ResourceManager.getSprite("terrain.overlay.road."+var_neighbourID+".");
							gfx.drawImage(overlay, imgx, imgy, null);
						}
					}
				}
				if(var_20!=-1) {
					gfx.setColor(Palette.defaultColors.get(var_20));
					gfx.fillRect(imgx+7, imgy+7, 2, 2);
				}
			}

			if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.MINING)
					&& !modeT
					&& drawImpr) {
				BufferedImage irrig = ResourceManager.getSprite("terrain.overlay.mining.");
				gfx.drawImage(irrig, imgx, imgy, null);
			}

			if(wm.getGameSave()!=null) {
				AbstractGameState gs = wm.getGameSave();
				if(drawSpecial && isSpecialResource(x,y,gs.getRandomSeed())) {
					BufferedImage irrig = ResourceManager.getSprite("terrain.overlay."+var_terrainType.getCode()+".special.");
					gfx.drawImage(irrig, imgx, imgy, null);
				}

				if(drawHuts && isHut(x,y,gs.getRandomSeed(),false,null)) {
					BufferedImage irrig = ResourceManager.getSprite("terrain.overlay.hut.");
					gfx.drawImage(irrig, imgx, imgy, null);
					if(wm.getTerrain(x, y).equals(TerrainType.OCEAN)) {
						gfx.setColor(new Color(84, 72, 160, 191));
						gfx.fillRect(imgx+1, imgy+1, 14, 13);
					} else if(gs.civ(0).canSee(new MapLocation(x,y))) {
						gfx.setColor(new Color(192,0,0,127));
						gfx.fillRect(imgx+1, imgy+1, 14, 13);
					}
				}
			}

			if(drawImpr && wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.FORTRESS)) {
				BufferedImage fortress = ResourceManager.getSprite("terrain.overlay.fortress.");
				gfx.drawImage(fortress, imgx, imgy, null);
			}

			if(wm.getGameSave()!=null) {
				AbstractGameState gs = wm.getGameSave();
				if(/*!showAll &&*/ drawShadow) {
					for(int var_neighbourID = 1; var_neighbourID<=8; var_neighbourID+=2) {
						int nx = CivLogic.alignXinMapRange(x+CivUtils.relCitySquareX3[var_neighbourID]);
						int ny = y+CivUtils.relCitySquareY3[var_neighbourID];
						//if(!gs.getPlayerCiv().canSee(nx, ny)) {
						if(!gs.player().canSee(new MapLocation(nx, ny))) {
							BufferedImage border = ResourceManager.getSprite("terrain.overlay.border."+(var_neighbourID+1)/2+".");
							gfx.drawImage(border, imgx, imgy, null);
						}
					}
				}

				/* DRAW CITY */
				if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.CITY)
						&& !modeT
						&& drawCity) {
					drawCity(gs.getCityAt(x, y),/* x, y,*/ bi, bi, imgx, imgy, showAll, false);
				}
			}
		} else {
			if(wm.hasTerrainImprovement(x, y, showAll, TerrainImprovementType.CITY)) {
				int owner = ((SVECiv) wm.owner(x, y)).getID();
				gfx.setColor(ImageUtils.civColors[owner][0]);
			} else {
				if(wm.getTerrain(x, y).equals(TerrainType.OCEAN)) {
					gfx.setColor(Palette.defaultColors.get(1));
				} else {
					gfx.setColor(Palette.defaultColors.get(2));
				}
			}
			gfx.fillRect(imgx, imgy, 4, 4);
		}
	}

	public static int alignXinMapRange(int x) {
		if(x>=0) {
			if(x<80) {
				return x;
			} else{
				return x-80; // only works if x<160
			}
		} else {
			return x+80; // only works if x>=-80
		}
	}
	public static boolean isInMap(int x, int y) {
		return (x>=0&&x<80&&y>=0&&y<50);
	}

	public static void colorReplace(BufferedImage bi, int x, int y, int w, int h, Color oldcol, Color newcol){
		for(int a=x;a<x+w;a++) {
			for(int b=y;b<y+h;b++) {
				if(bi.getRGB(a, b)==oldcol.getRGB()) {
					bi.setRGB(a, b, newcol.getRGB());
				}
			}
		}
	}

	public static SVEUnit determineUnitToDraw(AbstractGameState gs, int x, int y, SVEUnit _SVEUnit) {
		SVEUnit result = null;

		if(!gs.getTerrain(x, y).equals(TerrainType.OCEAN)
				|| !_SVEUnit.hasNextInStack()
				|| _SVEUnit.getType().category().equals(UnitCategory.SEA)) {
			result = (AbstractSVEUnit)computeBestDefenseInUnitStack(gs,x,y,_SVEUnit)[0];
		} else {
			SVEUnit nextUnit = _SVEUnit;
			SVEUnit startUnit = _SVEUnit;
			do {
				nextUnit = nextUnit.nextInStack();
				if(nextUnit.getType().category().equals(UnitCategory.SEA)) {
					result = nextUnit;
				}
				//System.out.println("1");
			} while(result == null && !nextUnit.equals(startUnit));
			if(result==null) {
				result = startUnit;
			}
		}
		return result;
	}

	// function below returns { Unit, Integer }, for best unit and corresponding defense value
	public static Object[] computeBestDefenseInUnitStack(AbstractGameState gs, int x, int y, SVEUnit _SVEUnit) {
		Object[] result = { _SVEUnit, -1 };
		int startUnitID = _SVEUnit.getID();
		int counter = 0;
		do {

			TerrainType tt = gs.getTerrain(x, y);
			if(!tt.equals(TerrainType.OCEAN) || _SVEUnit.getType().category().equals(UnitCategory.SEA)) {
				if(_SVEUnit.getType()!=null) {
					int def = _SVEUnit.getType().defense();
					if(_SVEUnit.getType().category().equals(UnitCategory.LAND)) {
						if(_SVEUnit.getStatus().equals(UnitStatus.FORTIFIED)) {
							def *= 3;
						} else {
							def *= 2;
						}
						def *= tt.getAttributes(CivLogic.isSpecialResource(x, y, gs.getRandomSeed()))[1];
						def = def<<3;
					} else {
						def = def<<4;
					}
					if(_SVEUnit.isVeteran()) {
						def = (def+(def>>2));
					}
					if(def>(Integer)result[1]) {
						result[1] = def;
						result[0] = _SVEUnit;
					}
				}
			}

			_SVEUnit = _SVEUnit.nextInStack();
			counter++;
			//System.out.println("0");
		} while(_SVEUnit!=null && _SVEUnit.getID()!=startUnitID && counter<10);

		return result;
	}
	public static Object[] computeBestDefenseInUnitStack(AbstractGameState gs, int x, int y, int civid, int unitid) {
		return computeBestDefenseInUnitStack(gs,x,y,((SVECiv) gs.civ(civid)).unit(unitid));
	}

	public static void resetStrategicLocations(Civ civ, int originalStatus, int x, int y, int range) {
		if(civ!=null) {
			AbstractGameState gs = ((Entity)civ).gamesave();
			if(gs!=null) {
				for(int i=0;i<16;i++) {
					int s = (Byte)((Entity) civ).getValue("geostrategy"+i+".active");
					int sx = (Short)((Entity) civ).getValue("geostrategy"+i+".position_x");
					int sy = (Short)((Entity) civ).getValue("geostrategy"+i+".position_y");
					if(s==originalStatus) {
						if(CivLogic.distance(x, sx, y, sy)<=range) {
							((Entity) civ).setPropertyValue("geostrategy"+i+".active",(byte)0xFF);
						}
					}
				}
			}
		}
	}

	public static void putUnitAt(AbstractSVEUnit _SVEUnit, int x, int y) {
		AbstractGameState gs = ((Entity) _SVEUnit.owner()).gamesave();
		Map map = gs;
		SVEUnit unit0 = getUnitAt(gs,x, y);
		insertUnitInStack(_SVEUnit,unit0);
		map.setOwner(x, y, _SVEUnit.owner());
		map.setOccupied(x, y, true);
	}

	public static SVEUnit getUnitAt(AbstractGameState gs, int x, int y) {
		Civ owner = CivLogic.whatCivOccupies(gs, x, y);
		if(owner!=null) {
			for(int var_unitID=0;var_unitID<128;var_unitID++) {
				SVEUnit u = ((SVECiv) owner).unit(var_unitID);
				if(u.exists() && u.getLocation().x()==x && u.getLocation().y()==y) {
					return u;
				}
			}
		}
		return null;
	}

	public static void insertUnitInStack(AbstractSVEUnit added, SVEUnit unit0) {
		if(unit0!=null) {
			if(!unit0.hasNextInStack()) {
				unit0.setNextInStack(added);
				added.setNextInStack(unit0);
			} else {
				SVEUnit next = unit0.nextInStack();
				unit0.setNextInStack(added);
				added.setNextInStack(next);
			}
		} else {
			added.setNextInStack(null);
		}
	}

	public static int dseg_6716_len = 0;
	public static int countCityTaxCollected_dseg_F09A;
	public static int countCityResearchBulbs_dseg_7066;
	public static int city_happyCitizenCount_dseg_7062;
	public static int city_unhappyCitizenCount_dseg_7064;
	public static int dseg_8F98;

	//seg007_6CE6:
	public static int adjustCitizenHappiness(City city, int specialistCount) {
		//seg007_6CE6:
		city_happyCitizenCount_dseg_7062 = rangeBound(city_happyCitizenCount_dseg_7062, 0, city.actualSize());
		
		while(true) {
			//seg007_6D3A:
			city_unhappyCitizenCount_dseg_7064 = rangeBound(city_unhappyCitizenCount_dseg_7064, 0, city.actualSize());

			if(rangeBound(city.actualSize()-specialistCount,0,99)>=city_happyCitizenCount_dseg_7062+city_unhappyCitizenCount_dseg_7064) return rangeBound(city.actualSize()-specialistCount,0,99);

			city_happyCitizenCount_dseg_7062--;
			city_happyCitizenCount_dseg_7062 = rangeBound(city_happyCitizenCount_dseg_7062, 0, city.actualSize());
			
			city_unhappyCitizenCount_dseg_7064--;
		}
	}
	public static int findPathLength(Map wm, int x1, int y1, int x2, int y2, boolean land, int maxmoves) {
		if(Math.abs(x1-x2)<7
				||Math.abs(y1-y2)<7) {
			// Create a fake militia or sail at (x1,y1)
			Civ civ0 = wm.getGameSave().civ(0);
			SVEUnit u127 = ((SVECiv) civ0).unit(127);

			((Entity) civ0).setPropertyValue("unit127.position_x", (Short)(short)x1);
			((Entity) civ0).setPropertyValue("unit127.position_y", (Short)(short)y1);
			((Entity) civ0).setPropertyValue("unit127.type", (Short)(short)(land?1:16));

			int next = findNextPathSquare(u127,maxmoves,x2,y2);

			((Entity) civ0).setPropertyValue("unit127.type", (Short)(short)(0xFF));

			if(next==-1) {
				return -1;
			} else {
				return dseg_6716_len;
			}
		} else {
			return -1;
		}
	}

	public static int findNextPathSquare(SVEUnit u127, int maxmoves, int destx, int desty) {
		int[] dseg_6516_unkX99 = new int[1000];
		int[] dseg_6616_unkY99 = new int[1000];

		dseg_6716_len = 0;
		int dseg_6718_unkx = 0;
		int dseg_671A_unky = 0;
		int[] dsegc6A0_grid16x16 = new int[16*16];

		int startX = u127.getLocation().x();
		int startY = u127.getLocation().y();

		int topleftX = destx-8;
		int topleftY = desty-8;

		if(destx!=dseg_6718_unkx
				|| desty!=dseg_671A_unky
				|| dsegc6A0_grid16x16[16*(startX-topleftX)+(startY-topleftY)]==0) {
			// seg016c7F
			dseg_6718_unkx = destx;
			dseg_671A_unky = desty;
			Arrays.fill(dsegc6A0_grid16x16,0);
			int var_2 = 0;
			int var_4 = 0;
			dseg_6516_unkX99[0] = destx;
			var_2++;
			dseg_6616_unkY99[0] = desty;

			dsegc6A0_grid16x16[16*(destx-topleftX)+desty-topleftY] = 1;

			int var_14 = 0;
			dseg_6716_len = maxmoves;

			int var_landOrSeaUnit = (u127.getType().category().equals(UnitCategory.SEA)?1:0);
			int var_totalMovesFlag = (u127.getType().totalMoves()==1?1:0);

			do{
				int var_unkX_9 = dseg_6516_unkX99[var_4];
				int var_unkY_9 = dseg_6616_unkY99[var_4];
				var_4++;
				int var_E = dsegc6A0_grid16x16[(var_unkX_9-topleftX)*16+var_unkY_9-topleftY];

				int var_1A = -1;
				if(var_E<=dseg_6716_len) {
					if(alignXinMapRange(var_unkX_9)!=startX
							||var_unkY_9!=startY) {
						//int var_impr = ((Entity)unit).gamesave().getTerrainImprovementsFlag(alignXinMapRange(var_unkX_9), var_unkY_9, true);
						int var_nid = 1;
						while(var_nid<=8) {
							int nx = var_unkX_9+CivUtils.relCitySquareX3[var_nid];
							if(Math.abs(nx-destx)<8) {
								int anx = alignXinMapRange(nx);
								int ny = var_unkY_9+CivUtils.relCitySquareY3[var_nid];
								if(Math.abs(ny-desty)<8) {
									if(isInMap(anx, ny)) {
										TerrainType ntt = ((Entity)u127).gamesave().getTerrain(anx, ny);
										if(var_landOrSeaUnit==(ntt.equals(TerrainType.OCEAN)?1:0)
												|| ((Entity)u127).gamesave().hasTerrainImprovement(anx, ny, false, TerrainImprovementType.CITY)) {
											if(((Entity)u127).gamesave().getAllImprovements(alignXinMapRange(var_unkX_9), var_unkY_9, true).size()==0
													|| ((Entity)u127).gamesave().getAllImprovements(anx, ny, true).size()==0) {
												if(var_totalMovesFlag==0) {
													var_1A = var_E + 3*ntt.movementCost(false);
												} else {
													var_1A = var_E + 3;
												}
											} else {
												var_1A = var_E+1;
											}
											int var_38 = dsegc6A0_grid16x16[(nx-topleftX)*16+ny-topleftY];
											if(var_38==0
													|| var_38>var_1A) {
												dsegc6A0_grid16x16[(nx-topleftX)*16+ny-topleftY] = var_1A;
												dseg_6516_unkX99[var_2] = nx;
												dseg_6616_unkY99[var_2] = ny;
												var_2++;
											}
										}
									}
								}
							}
							var_nid++;
						}
					} else {
						dseg_6716_len = var_E;
					}
				}
			} while(var_2!=var_4 && var_4<225);
		}
		int var_lengthToNext = -1;
		int var_1A = -1;
		int var_next = -1;
		if(maxmoves>dseg_6716_len) {
			int var_A = 99;
			int nid = 1;
			while(nid<=8) {
				int nx = startX+CivUtils.relCitySquareX3[nid];
				if(Math.abs(nx-destx)>=72) {
					if(nx<=destx) {
						nx += 80;
					} else {
						nx -= 80;
					}
				}
				if(Math.abs(nx-destx)<8) {
					int anx = alignXinMapRange(nx);
					int ny = startY + CivUtils.relCitySquareY3[nid];
					if(Math.abs(ny-desty)<8) {
						if(
								//(unit.getGameSave().getMap().getTerrain(anx, ny).equals(TerrainType.OCEAN)?1:0)
								//==
								//(unit.category().equals(UnitCategory.SEA)?1:0)
								!
								(((Entity)u127).gamesave().getTerrain(anx, ny).equals(TerrainType.OCEAN)
										^
										u127.getType().category().equals(UnitCategory.SEA))
										|| ((Entity)u127).gamesave().hasTerrainImprovement(anx, ny, false, TerrainImprovementType.CITY)
								) {
							int var_6 = dsegc6A0_grid16x16[(nx-topleftX)*16+ny-topleftY];
							if(var_6!=0) {
								if(var_6<var_A) {
									var_A = var_6;
									var_next = nx;
									//Object[] var_12 = JCivLogic.getUnitAt(unit.getGameSave(), anx, ny);
									//if(var_12==null) {
									var_lengthToNext = 0;
									//} else {
									//	var_lengthToNext = 16; // unit count *4
									//}
									var_lengthToNext += distance(destx, anx, desty, ny);
								}
								if(var_6 == var_A) {
									//Object[] var_12 = JCivLogic.getUnitAt(unit.getGameSave(), anx, ny);
									//if(var_12==null) {
									var_1A = 0;
									//} else {
									//	var_1A = 16; // unit count *4
									//}
									var_1A += distance(destx, anx, desty, ny);
									if(var_1A<var_lengthToNext) {
										var_next = nid;
										var_lengthToNext = var_1A;
									}
								}
							}
						}
					}
				}
				nid++;
			}
			if(var_next!=-1) {
				return var_next;
			}
		}
		if(var_next==-1) {
			destx = (Short)((Entity) u127).getValue("gotox");
			desty = (Short)((Entity) u127).getValue("gotoy");
		}
		return -1;
	}

	public static byte[] computePathFind(Map wm, boolean land) {
		byte[] unknown25 = new byte[260];
		Arrays.fill(unknown25,(byte)0);

		//int ax=0;

		for(int tileX=0;tileX<20;tileX++) {
			for(int tileY=0;tileY<13;tileY++) {
				int x = (tileX<<2)+1;
				int y = (tileY<<2)+1;
				int cid = -1;

				int x2 = -1;
				int y2 = -1;
				if(land^wm.getTerrain(x, y).equals(TerrainType.OCEAN)) {
					cid = wm.landMassID(x, y);
					x2 = x;
					y2 = y;
				} else if (land^wm.getTerrain(x+1, y).equals(TerrainType.OCEAN)) {
					cid = wm.landMassID( x+1, y);
					x2 = x+1;
					y2 = y;
				} else if (land^wm.getTerrain(x, y+1).equals(TerrainType.OCEAN)) {
					cid = wm.landMassID( x, y+1);
					x2 = x;
					y2 = y+1;
				} else if (land^wm.getTerrain(x+1, y+1).equals(TerrainType.OCEAN)) {
					cid = wm.landMassID( x+1, y+1);
					x2 = x+1;
					y2 = y+1;
				}

				if(cid!=-1 && ((x2>1 && x2<78 && y2>1 && y2<48)||!land)) {
					for(int a=1;a<=4;a++) {
						int rx = CivUtils.relCitySquareX3[a];
						int ry = CivUtils.relCitySquareY3[a];
						int x4 = x + 4*rx;
						int y4 = y + 4*ry;
						int cid2 = -1;

						int x3 = -1;
						int y3 = -1;
						if(land^wm.getTerrain(x4, y4).equals(TerrainType.OCEAN)) {
							cid2 = wm.landMassID( x4, y4);
							x3 = x4;
							y3 = y4;
						} else if(land^wm.getTerrain(x4+1, y4).equals(TerrainType.OCEAN)) {
							cid2 = wm.landMassID( x4+1, y4);
							x3 = x4+1;
							y3 = y4;
						} else if(land^wm.getTerrain(x4, y4+1).equals(TerrainType.OCEAN)) {
							cid2 = wm.landMassID( x4, y4+1);
							x3 = x4;
							y3 = y4+1;
						} else if(land^wm.getTerrain(x4+1, y4+1).equals(TerrainType.OCEAN)) {
							cid2 = wm.landMassID( x4+1, y4+1);
							x3 = x4+1;
							y3 = y4+1;
						}

						if(cid==cid2 && (( x3>1 && x3<78 && y3>1 && y3<48)||!land)) {
							int pathLength = findPathLength(wm, x2, y2, x3, y3, land, 20);
							if(pathLength!=-1 && pathLength<20) {
								unknown25[((tileY+13)%13)+13*((tileX+20)%20)] |= (1<<(a-1));
								unknown25[((tileY+CivUtils.relCitySquareY3[a]+13)%13)
								          +13*((tileX+CivUtils.relCitySquareX3[a]+20)%20)] |= (1<<((a+3)&7));
							}
						}
					}
				}
			}
		}

		return unknown25;
	}


	public static void manageUnitArrival(AbstractSVEUnit _SVEUnit, int x, int y, int mode0) {
		AbstractGameState gs = ((Entity)_SVEUnit).gamesave();
		Civ player = gs.player();
		Map map = gs;
		Civ civ = _SVEUnit.owner();
		TerrainType tt = map.getTerrain(x, y);
		if(civ.equals(player)) {
			for(int nid = 0; nid<25; nid++) {
				int nx = CivLogic.alignXinMapRange(x+CivUtils.relCitySquareX3[nid]);
				int ny = y+CivUtils.relCitySquareY3[nid];
				if(
						nid<9 // inner ring, 8 first squares
						||
						((_SVEUnit.sightRange()&2)!=0 // has far sight (can see outer ring)
						&& ( !_SVEUnit.category().equals(UnitCategory.SEA) // is not maritime
								|| map.getTerrain(nx,  ny).equals(TerrainType.OCEAN)) // or on ocean and maritime
								)
						) {
					if(CivLogic.isInMap(nx, ny)) {
						gs.setVisibility(civ, nx, ny, 1, 1, VisibilityType.VISIBLE);
						map.actualizeVisibleImprovements(nx, ny, 1, 1);
					}
				}
			}
		}
		//int impr = map.getTerrainImprovementsFlag(x, y, true);
		for(int nid = 1; nid<9; nid++) {
			int nx = CivLogic.alignXinMapRange(x+CivUtils.relCitySquareX3[nid]);
			int ny = y+CivUtils.relCitySquareY3[nid];
			if(CivLogic.isInMap(nx, ny)) {
				if(!civ.equals(gs.civ(0))) { // Barbarians do not reveal shadow
					gs.setVisibility(civ, nx, ny, 1, 1, VisibilityType.VISIBLE);
				}
				//int nimpr = map.getTerrainImprovementsFlag(nx, ny, true);
				if(map.hasTerrainImprovement(nx,  ny,  true, TerrainImprovementType.CITY)
						&&(!gs.owner(nx,ny).equals(civ))) {
					//seg005_3FF
					Civ nowner = gs.owner(nx, ny);
					City ncity = gs.getCityAt(nx, ny);

					unkCityProcess27_28(ncity);

					if(civ.equals(player)) {
						ncity.visibleSize(ncity.actualSize());
					}

					if(_SVEUnit.category().equals(UnitCategory.LAND)) {
						_SVEUnit.setVisibleBy(nowner,true);
					}
					int diplo = nowner.diplomaticStatusWith(civ);
					if((diplo&2)==0) { // Not at war?
						// TODO: strategic position update
						seg010_34C8(nowner,nx,ny,1,4);
						seg010_34C8(nowner,nx,ny,2,2);
					}
				}
				SVEUnit nunit = getUnitAt(gs,nx,ny);
				if(nunit!=null) {
					Civ nciv = nunit.owner();
					if(nciv!=null&&!nciv.equals(civ)) {
						if(!map.hasTerrainImprovement(x,  y,  true, TerrainImprovementType.CITY)) {
							_SVEUnit.setVisibleBy(nciv, true);
						}
						// TODO: wake sentried units on neighbour square
						TerrainType ntt = map.getTerrain(nx, ny);
						if(ntt.equals(TerrainType.OCEAN)||!tt.equals(TerrainType.OCEAN)) {
							((Entity) nunit).setPropertyValue("gotox",(Short)(short)0xFF);
						}
						if(ntt.equals(TerrainType.OCEAN)||!tt.equals(TerrainType.OCEAN)
								||_SVEUnit.category().equals(UnitCategory.AIR)) {
							_SVEUnit.setPropertyValue("gotox",(Short)(short)0xFF);
						}

						if(!map.hasTerrainImprovement(nx,  ny,  true, TerrainImprovementType.CITY)) {
							nunit.setVisibleBy(civ, true);
						}
						if(_SVEUnit.category().equals(UnitCategory.LAND)
								&& !tt.equals(TerrainType.OCEAN)
								&& !ntt.equals(TerrainType.OCEAN)
								&& !nunit.getType().category().equals(UnitCategory.AIR)) {
							//IN PROGRESS - WIP
						}
					}
				}
			}
		}

	}

	public static void seg010_34C8(Civ civ, int x, int y, int n1, int n2) {
		for(int loop=0;loop<32;loop++) {
			//TODO: things related with non-SVE data "unkown 32", a set of 32 strategic positions...
		}
	}

	public static void unkCityProcess27_28(City city) {
		if(city.owner().equals(((Entity)city).gamesave().player())) {
			// Skipped: unknown flag set to 1, at dseg_20F4
			for(String sb : new String[]{"27", "28"} ) {
				int ub = ((Short)((Entity) city).getValue("unknown_cb"+sb))&0xFF;
				if(ub!=0xFF) {
					int tid = ub&0b111111;
					AbstractSVEUnit _SVEUnit = CivLogic.createUnit(city.owner(), ((Entity)city).gamesave().unitType(tid), city.getLocation().x(), city.getLocation().y());
					if(_SVEUnit!=null) {
						_SVEUnit.setStatus(UnitStatus.FORTIFIED);
						if((ub&0b1000000)!=0) {
							_SVEUnit.setVeteran(true);
						}
						((Entity) city).setPropertyValue("unknown_cb"+sb,(Short)(short)0xFF);
					}
				}
			}
			// Skipped: unknown flag set to 0, at dseg_20F4
			city.set(CityStatus.RAPTURE_1, true);
		}
	}


	public static void diplomacy(Civ civAI, int x, int y, int arg6, int arg2BE) {
		AbstractGameState gs = ((Entity)civAI).gamesave();
		Map map = gs;
		Civ player = gs.player();
		map.actualizeVisibleImprovements(x, y, 1, 1);
		// Skipped: draw map square terrain and units
		// Skipped: if English use "Queen" or "Empress" instead of "King" or "Emperor"
		int nextContact = (Integer)((Entity) civAI).getValue("contact_player_countdown");
		if(arg6==0) {
			((Entity) civAI).setPropertyValue("contact_player_countdown", (Short)(short)(int)(Integer)gs.getValue("game_turn"));
		}
		int unk0 = 0;
		int var_24 = 0;
		int unk1 = arg6;
		int var_2E = 0;
		int hostility = 0;
		int var_8 = 1;
		int unk2 = 0;

		for(int cid=01;cid<15;cid++) {
			if((Short)((Entity)civAI).getValue("continent"+cid+"_city_count")<=((Short)((Entity)civAI).getValue("city_count")>1?1:0)
					||(Short)((Entity)player).getValue("continent"+cid+"_attack")<=(Short)((Entity)civAI).getValue("continent"+cid+"_defense")) 
			{
				if((Short)((Entity)player).getValue("continent"+cid+"_defense")!=0
						&&(Short)((Entity)civAI).getValue("continent"+cid+"_defense")!=0) {
					var_24 = 1;
				}
				if((Short)((Entity)player).getValue("continent"+cid+"_city_count")==0) {
					if((Short)((Entity)civAI).getValue("continent"+cid+"_city_count")==0) {
						hostility += ((Short)((Entity)civAI).getValue("continent"+cid+"_attack")-(Short)((Entity)player).getValue("continent"+cid+"_attack"))/4;
					} else {
						hostility += ((Short)((Entity)player).getValue("continent"+cid+"_defense")-(Short)((Entity)player).getValue("continent"+cid+"_attack"))/2;
					}
				} else {
					if((Short)((Entity)civAI).getValue("continent"+cid+"_city_count")<=1) {
						hostility += (Short)((Entity)civAI).getValue("continent"+cid+"_attack");
					} else {
						hostility += ((Short)((Entity)civAI).getValue("continent"+cid+"_attack")-(Short)((Entity)player).getValue("continent"+cid+"_attack"));
					}
				}
			} else {
				var_2E += (!gs.getDifficultyLevel().equals(DifficultyLevel.CHIEFTAIN)?2:4)*
						(Short)((Entity)player).getValue("continent"+cid+"_attack")/(1+(Short)((Entity)civAI).getValue("continent"+cid+"_defense"));
			}
		}

		int var_4 = 0;
		if((Short)((Entity)player).getValue("ranking")==7 // player is the best Civ in the world
				&& (Short)((Entity)player).getValue("city_count")>4 // player has more than 4 cities
				&& (Short)((Entity)civAI).getValue("city_count")>1 // AI has more than 1 city
				&& (Short)((Entity)player).getValue("active_units25")==0 // player does NOT have nukes
				&& (Integer)((Entity)gs).getValue("game_turn")>200 // after 1000 AD ?
				) { 
			var_4 = 1;
		}
		if(var_4!=0
				||
				((((Integer)civAI.diplomaticStatusWith(player))&8)!=0 // vendetta
				&& (3*(Short)((Entity)civAI).getValue("military_power"))<=(Short)((Entity)player).getValue("military_power"))
				) {
			if((Short)((Entity)player).getValue("active_units25")==0) {
				var_24 = 1;
				hostility = CivLogic.rangeBound(hostility, 10*(short)((Entity)civAI).getValue("ranking"), 9999);
				var_2E = 0;
			}
		}
		// ovr06_2A0
		if((Short)((Entity)civAI).getValue("active_units25")!=0) {
			int sflag = civAI.diplomaticStatusWith(player);
			civAI.setDiplomaticStatusWith(player, sflag | 0x80);
		}
		// ovr06_2C1
		if((Short)((Entity)civAI).getValue("active_units25")!=0
				&&((Short)((Entity)player).getValue("active_units25")==0)) {
			var_24 = 1;
			hostility = CivLogic.rangeBound(hostility, 100, 9999);
		}
		// ovr06_2FB
		if((Short)((Entity)player).getValue("active_units25")!=0) {
			if((Short)((Entity)civAI).getValue("active_units25")!=0) {
				hostility = (Short)((Entity)civAI).getValue("active_units25")*(hostility/2)/(Short)((Entity)player).getValue("active_units25");
			} else {
				var_2E = 4;
			}
		}
		// ovr06_34C
		if(var_2E!=0) {
			var_24 = 0;
			var_8 = 0;
		}
		// ovr06_35C
		if(CivLogic.isWonderApplicable(player, WonderType.GREAT_WALL)
				|| CivLogic.isWonderApplicable(player, WonderType.UNITED_NATIONS)) {
			var_4 = 0;
			var_24 = 0;
			if((nextContact&0xFFFF)==0xFFFE) {
				nextContact = 0xFFFF;
			}
		}
		//ovr06_397
		if((civAI.diplomaticStatusWith(player)&8)==0) { // no vendetta ?
			if((nextContact&0xFFFF)==0xFFFE) {
				hostility = hostility/4;
			}
		} else {
			hostility = hostility *2;
		}
		//ovr06_3C2
		hostility = 50*CivLogic.rangeBound((hostility*(gs.getDifficultyLevel().getID()+1))/32,0,20);
		if(hostility>(Short)((Entity)player).getValue("cash")
				&& hostility<=2*(Short)((Entity)player).getValue("cash")
				&& (Short)((Entity)player).getValue("cash")>=50) {

		}
		//ovr06_417
		if((Short)((Entity)civAI).getValue("active_units25")!=0) {
			if(((Integer)civAI.diplomaticStatusWith(player)&0x80)==0
					&& (Short)((Entity)player).getValue("cash")<hostility) {
				hostility = ((Short)((Entity)player).getValue("cash")/50)*50;
			}
			//over06_452
			int sflag = civAI.diplomaticStatusWith(player);
			civAI.setDiplomaticStatusWith(player, sflag | 0x80);
		}
		// ovr06_464
		if(hostility==0
				|| (3*(Short)((Entity)civAI).getValue("military_power"))<(Short)((Entity)player).getValue("military_power")) {
			var_24 = 0;
		}
		//ovr06_487
		if(var_24!=0) {
			var_8 = 2;
		}
		if(var_24==0
				&& (civAI.diplomaticStatusWith(player)&0x4)!=0) {
			var_8 = 0;
		}
		//ovr06_4B1
		int var_10 = var_24;
		int var_2A = 0;
		if((civAI.diplomaticStatusWith(player)&8)!=0) {
			var_2A = -2;
		}
		int var_30 = -1;
		int loopCivID = 1;
		do {
			Civ loopCiv = gs.civ(loopCivID);
			if(loopCivID!=((SVECiv) player).getID()
					&& (civAI.diplomaticStatusWith(gs.civ(loopCivID))&0x3)==1) {
				if((Short)((Entity)loopCiv).getValue("military_power")*4>(Short)((Entity)civAI).getValue("military_power")) {
					var_2A++;
				}
				if((Short)((Entity)loopCiv).getValue("military_power")>(Short)((Entity)civAI).getValue("military_power")) {
					var_2A++;
				}
				if((player.diplomaticStatusWith(loopCiv)&0x2)!=0) {
					var_30 = loopCivID; // here, var_30 is the ID of a Civ in peace with player, and at war with AI civ
				}
			}
			loopCivID++;
		}while(loopCivID<8);

		//ovr06_54D (no name in IDA)
		var_2A = var_2A - civAI.diplomaticAttitude().id();

		if((Short)((Entity)player).getValue("military_power")<(Short)((Entity)civAI).getValue("military_power")) {
			var_2A--;
		}

		//ovr06_575
		//if(civAI.diplomaticStatusWith(player)&0x2)
	}

	public static int refreshLandValues(Map map, boolean useCivLogic) {
		//int[] vals1 = new int[(78-2)*(48-2)];
		//int[] vals3 = new int[(78-2)*(48-2)];
		int totalWorldValue = 0;
		//		int[] nvals = new int[80*50];
		//		Arrays.fill(nvals,-1);
		//int[] nvals = null;
		for(int x=2;x<78;x++) {
			for(int y=2;y<48;y++) {
				int TMW = 0x10000;
				if(map.getGameSave()!=null) TMW = map.getGameSave().getRandomSeed();
				int landValue = CivLogic.computeLandValue(x, y, map, TMW, useCivLogic);
				//vals3[(y-2)*76+(x-2)] = landValue;
				map.setTerrainValue(x, y, true, landValue);
				totalWorldValue += landValue; 

				//				int c = this.getRawLayerValue(1, x, y);
				//				if(c==0||c>=8) {
				//					if(c==8) { // possibly a "city shroud"
				boolean city = false;
				for(int i=0;i<45&&!city;i++) {
					int dx = CivUtils.relCitySquareX3[i];
					int dy = CivUtils.relCitySquareY3[i];
					int nx = CivLogic.alignXinMapRange(x+dx);
					int ny = y+dy;
					if(CivLogic.isInMap(nx, ny) && map.hasTerrainImprovement(nx, ny, true, TerrainImprovementType.CITY)) {
						city=true;
					}
				}
				if(!city) {
					//vals1[(y-2)*76+(x-2)] = landValue;
					map.setTerrainValue(x, y, false, landValue);
				} else {
					//vals1[(y-2)*76+(x-2)] = map.getRawLayerValue(1, x, y);
					map.setTerrainValue(x, y, false, map.getTerrainValue(x, y, false));
				}
				//					}
				//					
				//				} else {
				//					vals1[(y-2)*76+(x-2)] = c;
				//				}
			}
		}
		//map.setLayerValues(2, 2, 76, 46, 1, vals1);
		//map.setLayerValues(2, 2, 76, 46, 3, vals3);
		return totalWorldValue;
	}
	public static int computeLandValue(int x, int y, Map wm, int TMW, boolean useCivLogic) {
		boolean debug=false&&(x==48&&y==32);

		if(debug)		System.out.println("Compute land value: x="+x+", y="+y);
		short value = 0;

		TerrainType terrain = wm.getTerrain(x, y);
		if(debug)		System.out.println("   terrain is "+terrain.toString());
		if(terrain!=TerrainType.RIVER
				&& terrain!=TerrainType.GRASSLANDS
				&& terrain!=TerrainType.PLAINS) {
			value = 0;
		} else {
			// Process neighbour "city" squares
			short relValue = 0;
			for(int i=0;i<=20;i++) {
				relValue = 0;
				if(debug)		System.out.println("      checking neighbour "+i+" ("+CivUtils.relCityX[i]+","+CivUtils.relCityY[i]+")");
				int relX = x+CivUtils.relCityX[i];
				int relY = y+CivUtils.relCityY[i];

				//				if(neighbourCache!=null && neighbourCache[relY*80+relX]!=-1) {
				//					relValue = (short) neighbourCache[relY*80+relX];
				//				} else {
				TerrainType relT = wm.getTerrain(relX, relY);
				if(debug)		System.out.print("         neighbour type: "+relT);

				boolean special = false;
				//int statsId = relT.getMsgID();
				if(relT==TerrainType.RIVER || relT==TerrainType.GRASSLANDS) {
					if(debug)		System.out.println("");
					if (((relX+relY)%4) == 0 || ((relX+relY)%4) == 3) {
						relValue += 2;
						if(debug)		System.out.println("         special river/grassland -> +2 -> relVal = "+relValue);
					}
				} else if(TMW<Short.MAX_VALUE) if (isSpecialResource(relX, relY, TMW)){
					//relValue += 12;
					//System.out.println("         special resource -> +12 -> relVal = "+relValue);
					if(debug)		System.out.print(" + SEPCIAL");
					if(debug)		System.out.println("");

					//statsId += 12;
					special = true;
				} else {
					if(debug)		System.out.println("");
				}



				relValue += relT.getScore(special);
				if(debug)		System.out.println("         terrainScore -> +"+relT.getScore(special)+" ("+relT.getMsgID()+") -> relVal = "+relValue);

				if( (i<9 && useCivLogic) || (i<8 && !useCivLogic) ) { // inner circle
					relValue *= 2;
					if(debug)		System.out.println("         inner circle -> x2 -> relVal = "+relValue);
				}
				if( (i==0 && useCivLogic) || (i==20 && !useCivLogic) ) {
					relValue *= 2;
					if(!useCivLogic) relValue *= 2;
					if(debug)		System.out.println("         i == 0 (?) -> x2 -> relVal = "+relValue);
				}
				//					if(neighbourCache!=null) neighbourCache[relY*80+relX] = relValue;
				//				}

				value += relValue;
				if(debug)		System.out.println("   TOTAL VALUE so far: "+Utils.dhInt(value));
			}

			if(terrain==TerrainType.RIVER || terrain==TerrainType.GRASSLANDS) {
				if (!(((x+y)%4) == 0 || ((x+y)%4) == 3)) {
					value -= 16;
					if(debug)		System.out.println("   Central terrain is grassland/river but NOT special -> -16 -> value = "+value);
				}
			}

			int dxax = value - 120;
			short _ax = (short)dxax;
			short _dx = (short)(dxax>>16);
			_ax = (short) (_ax ^ _dx);
			_ax -= _dx;
			if(debug)		System.out.println("   abs(value -120) -> value = "+Utils.dbhInt(_ax));

			_ax >>= 3;
				_ax = (short) (_ax ^ _dx);
				_ax -= _dx;
				if(debug)		System.out.println("   abs(value/8) -> value = "+Utils.dbhInt(_ax));

				value = (_ax<1?1:_ax>15?15:_ax);
				if(debug)		System.out.println("   range bound to [1..15] -> value = "+value);

				relValue = (short) (((value+(value<0?1:0))>>1)+8);
				if(debug)		System.out.println("   final compute -> += (value+(value<0?1:0))/2+8 -> value = "+relValue);
				//relValue = Math.abs(relValue)/2+8;

				value = relValue;
		}


		return value;
	}


}