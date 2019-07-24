// CivOne
//
// To the extent possible under law, the person who associated CC0 with
// CivOne has waived all copyright and related or neighboring rights
// to CivOne.
//
// You should have received a copy of the CC0 legalcode along with this
// work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using CivOne.Advances;
using CivOne.Buildings;
using CivOne.Enums;
using CivOne.Leaders;
using CivOne.Tasks;
using CivOne.Tiles;
using CivOne.Units;

namespace CivOne
{
    // ReSharper disable once InconsistentNaming
    internal partial class AI : BaseInstance
	{
        private Player Player { get; }
        private ILeader Leader => Player.Civilization.Leader;

		internal void Move(IUnit unit)
		{
			if (Player != unit.Owner) 
                return;

			if (unit.Owner == 0)
			{
				BarbarianMove(unit);
				return;
			}

            switch (unit.Role)
            {
                case UnitRole.Settler:
                    SettlerMove(unit);
                    break;
                case UnitRole.Defense:
                    DefenseMove(unit);
                    break;
                case UnitRole.LandAttack:
                    LandAttackMove(unit);
                    break;
                case UnitRole.SeaAttack:
                case UnitRole.AirAttack:
                case UnitRole.Transport:
                case UnitRole.Civilian:
                    Game.DisbandUnit(unit);
                    break;
            }

		}

        private void SettlerMove(IUnit unit)
        {
            if (!(unit is Settlers)) 
                return;

            ITile tile = unit.Tile;

            bool hasCity = tile.City != null;
            bool validCity = (tile is Grassland || tile is River || tile is Plains) && !hasCity;
            bool validIrrigation = (tile is Grassland || tile is River || tile is Plains || tile is Desert) && !hasCity && (!tile.Mine) && (!tile.Irrigation) && tile.CrossTiles().Any(x => x.IsOcean || x is River || x.Irrigation);
            bool validMine = (tile is Mountains || tile is Hills) && !hasCity && (!tile.Mine) && (!tile.Irrigation);
            bool validRoad = !hasCity && tile.Road;
            int nearestCity = 255;
            int nearestOwnCity = 255;

            if (Game.GetCities().Any()) 
                nearestCity = Game.GetCities().Min(x => Common.DistanceToTile(x.X, x.Y, tile.X, tile.Y));
            if (Game.GetCities().Any(x => x.Owner == unit.Owner)) 
                nearestOwnCity = Game.GetCities().Where(x => x.Owner == unit.Owner).Min(x => Common.DistanceToTile(x.X, x.Y, tile.X, tile.Y));

            if (validCity && nearestCity > 3)
            {
                GameTask.Enqueue(Orders.FoundCity(unit as Settlers));
                return;
            }
            else if (nearestOwnCity < 3)
            {
                switch (Common.Random.Next(5 * nearestOwnCity))
                {
                    case 0:
                        if (validRoad)
                        {
                            GameTask.Enqueue(Orders.BuildRoad(unit));
                            return;
                        }
                        break;
                    case 1:
                        if (validIrrigation)
                        {
                            Debug.Assert(!(tile is Mountains));
                            GameTask.Enqueue(Orders.BuildIrrigation(unit));
                            return;
                        }
                        break;
                    case 2:
                        if (validMine)
                        {
                            GameTask.Enqueue(Orders.BuildMines(unit));
                            return;
                        }
                        break;
                }
            }

            for (int i = 0; i < 1000; i++)
            {
                int relX = Common.Random.Next(-1, 2);
                int relY = Common.Random.Next(-1, 2);
                if (relX == 0 && relY == 0) continue;
                if (unit.Tile[relX, relY] is Ocean) continue;
                if (unit.Tile[relX, relY].Units.Any(x => x.Owner != unit.Owner)) continue;
                if (!unit.MoveTo(relX, relY)) continue;
                return;
            }
            unit.SkipTurn();

        }

        private void DefenseMove(IUnit unit)
        {
            unit.Fortify = true;
            while (unit.Tile.City != null && 
                   unit.Tile.Units.Count(x => x.Role == UnitRole.Defense) > 2)
            {
                IUnit disband;
                IUnit[] units = unit.Tile.Units.Where(x => x != unit).ToArray();
                if ((disband = unit.Tile.Units.FirstOrDefault(x => x is Militia)) != null) 
                { Game.DisbandUnit(disband); continue; }
                if ((disband = unit.Tile.Units.FirstOrDefault(x => x is Phalanx)) != null) 
                { Game.DisbandUnit(disband); continue; }
                if ((disband = unit.Tile.Units.FirstOrDefault(x => x is Musketeers)) != null) 
                { Game.DisbandUnit(disband); continue; }
                if ((disband = unit.Tile.Units.FirstOrDefault(x => x is Riflemen)) != null) 
                { Game.DisbandUnit(disband); continue; }
                if ((disband = unit.Tile.Units.FirstOrDefault(x => x is MechInf)) != null) 
                { Game.DisbandUnit(disband); continue; }
            }
        }

        // Delta X/Y values for the 8 neighbor tiles - NOT the center
        private static int[] deltaX = { -1, 0, +1, -1, +1, -1, 0, +1 };
        private static int[] deltaY = { -1, -1, -1, 0, 0, +1, +1, +1 };

        private void LandAttackMove(IUnit unit)
        {
            // TODO seg010_13CB
            // if (bestLandValue != 0)
            //     seg010_1461: check for tribal hut
            // seg010_14b6: setStrategicLocation

            // TODO seg010_15AA / seg010_17A7
            // if (unit in city)
            //   distToClosestUnit > 0
            //      assignNewTacticalLocation

            bool isEnemyNearby = IsEnemyUnitNearby(unit);
            City nearCity = FindNearestCity(unit.X, unit.Y);
            int distNearCity = nearCity == null ? 
                                    int.MaxValue : 
                                    Common.Distance(nearCity.X, nearCity.Y, unit.X, unit.Y);

            // DarkPanda ai_orders seg010_2192
            // TODO is 'totalMoves' === MovesLeft or MovesLeft+PartMoves ?
            if (unit.MovesLeft < 2 &&
                distNearCity < 4 &&
                Human == nearCity.Owner &&
                // at war with human &&
                (unit.Tile.Irrigation || unit.Tile.Mine))
            {
                // pillage the square
                unit.Pillage(); // TODO why isn't this an Order ?
                return;
            }

            // DarkPanda ai_orders seg010_2857
            if (!unit.Goto.IsEmpty &&
                !isEnemyNearby)
            {
                return; //continue with goto unless enemy encountered
            }

            // seg010_2980 === var_5C
            bool enemyUnitOrCityNearby = IsEnemyUnitOrCityNearby(unit);

            // TODO seg010_29C5

            // seg010_29E7: neighbor loop
            int bestValue = int.MinValue;
            int bestNeighborId = 0;

            for (int neighborloop = 0; neighborloop < 8; neighborloop++)
            {
                // TODO map range check
                int neighX = unit.X + deltaX[neighborloop];
                int neighY = unit.Y + deltaY[neighborloop];

                var tile = Game.Map[neighX, neighY];
                if (tile.IsOcean)
                    continue; // ignore ocean tiles

                int neighOwner = getOwner(neighX, neighY);
                var neighUnits = Game.GetUnits(neighX, neighY);
                bool neighOwnUnits = neighUnits.Length > 0 && neighUnits[0].Owner == unit.Owner;
                bool neighEnemyUnits = neighUnits.Length > 0 && neighUnits[0].Owner != unit.Owner; // var_1C equivalent(?)

                // TODO seg010_2A7B: diplomat logic

                // TODO seg010_2B0D: skip this neighbor if too many enemies near?

                // TODO seg010_2C06: skip this neighbor if cannot stack?

                // TODO seg010_2C6D: visibility_flag case - NYI

                // seg010_2CE1
                int neighborValue = Common.Random.Next(5);
                if (neighOwnUnits)
                    neighborValue += aggregateUnitStackAttribute(neighUnits[0], 3) * 2 /
                                     (aggregateUnitStackAttribute(neighUnits[0], 1) + 1);
                else
                {
                    neighborValue += tile.Defense * 4;
                }

                // TODO seg010_2DF3 : AImilitaryPower

                // seg010_2E43
                if (neighEnemyUnits)
                {
                    // TODO seg010_2E6D: attempt to bribe unit under specific conditions

                }

                // seg010_31A5: neighbor unit(s) are our own
                if (neighOwnUnits)
                {
                    neighborValue -= unit.Defense;
                }

                if (neighUnits.Length < 1)
                {
                    // seg010_31C7: undefended enemy city, lets attack
                    var neighCity = Game.GetCity(neighX, neighY);
                    if (neighCity != null && neighCity.Owner != unit.Owner)
                        neighborValue = int.MaxValue;

                    // seg010_31EC: a hut is desirable
                    if (tile.Hut)
                        neighborValue += 20;
                }

                // seg010_3209
                if (!enemyUnitOrCityNearby)
                {
                    neighborValue += EvaluateNextTileOut(unit, neighX, neighY);
                }

                if (neighborValue > bestValue)
                {
                    bestValue = neighborValue;
                    bestNeighborId = neighborloop;
                }

            } // neighbor eval loop

            unit.MoveTo(deltaX[bestNeighborId], deltaY[bestNeighborId]);

            //RandomMove(unit);
        }

        // There were no enemies or cities nearby. Look to the neighbors of the neighbor tile,
        // and return the DELTA impact on the value of the neighbor.
        private int EvaluateNextTileOut(IUnit unit, int neighX, int neighY)
        {
            int neighborValueDelta = 0;

            // TODO seg010_3212 : don't know what the purpose of seg029_1498[] is

            // seg010_329D : determine if the direction we're going is toward ocean or other units
            for (int neighLoop2 = 0; neighLoop2 < 8; neighLoop2++)
            {
                int nnx = neighX + deltaX[neighLoop2];
                int nny = neighY + deltaY[neighLoop2];

                // TODO validate within map range

                // TODO visibility seg010_32EC

                if (!Map[nnx, nny].IsOcean)
                    neighborValueDelta += 2;
                if (Game.GetUnits(nnx, nny).Length > 0)
                    neighborValueDelta -= 2;
            }

            return neighborValueDelta;
        }

        private void RandomMove(IUnit unit)
        {
            for (int i = 0; i < 1000; i++)
            {
                if (unit.Goto.IsEmpty)
                {
                    int gotoX = Common.Random.Next(-5, 6);
                    int gotoY = Common.Random.Next(-5, 6);
                    if (gotoX == 0 && gotoY == 0) continue;
                    if (!Player.Visible(unit.X + gotoX, unit.Y + gotoY)) continue;

                    unit.Goto = new Point(unit.X + gotoX, unit.Y + gotoY);
                    continue;
                }

                if (!unit.Goto.IsEmpty)
                {
                    int distance = unit.Tile.DistanceTo(unit.Goto);
                    ITile[] tiles = unit.MoveTargets.OrderBy(x => x.DistanceTo(unit.Goto)).ThenBy(x => x.Movement).ToArray();
                    if (tiles.Length == 0 || tiles[0].DistanceTo(unit.Goto) > distance)
                    {
                        // No valid tile to move to, cancel goto
                        unit.Goto = Point.Empty;
                        continue;
                    }
                    else if (tiles[0].DistanceTo(unit.Goto) == distance)
                    {
                        // Distance is unchanged, 50% chance to cancel goto
                        if (Common.Random.Next(0, 100) < 50)
                        {
                            unit.Goto = Point.Empty;
                            continue;
                        }
                    }

                    if (tiles[0].Units.Any(x => x.Owner != unit.Owner))
                    {
                        if (unit.Role == UnitRole.Civilian || unit.Role == UnitRole.Settler)
                        {
                            // do not attack with civilian or settler units
                            unit.Goto = Point.Empty;
                            continue;
                        }

                        if (unit.Role == UnitRole.Transport && Common.Random.Next(0, 100) < 67)
                        {
                            // 67% chance of cancelling attack with transport unit
                            unit.Goto = Point.Empty;
                            continue;
                        }

                        if (unit.Attack < tiles[0].Units.Select(x => x.Defense).Max() && Common.Random.Next(0, 100) < 50)
                        {
                            // 50% of attacking cancelling attack of stronger unit
                            unit.Goto = Point.Empty;
                            continue;
                        }
                    }

                    if (!unit.MoveTo(tiles[0].X - unit.X, tiles[0].Y - unit.Y))
                    {
                        // The code below is to prevent the game from becoming stuck...
                        if (Common.Random.Next(0, 100) < 67)
                        {
                            unit.Goto = Point.Empty;
                            continue;
                        }
                        else if (Common.Random.Next(0, 100) < 67)
                        {
                            unit.SkipTurn();
                            return;
                        }
                        else
                        {
                            Game.DisbandUnit(unit);
                            return;
                        }
                    }

                    return;
                }
            }

            unit.SkipTurn();
            return;
        }

        private int aggregateUnitStackAttribute(IUnit unit, int p1)
        {
            // TODO NYI
            return 1;
        }

        private int getOwner(int x, int y)
        {
            var tile = Map[x, y];
            // TODO owner NYI
            return 0;
        }

        internal void ChooseResearch()
		{
			if (Player.CurrentResearch != null) return;
			
			IAdvance[] advances = Player.AvailableResearch.ToArray();
			
			// No further research possible
			if (advances.Length == 0) return;

			Player.CurrentResearch = advances[Common.Random.Next(0, advances.Length)];

			Log($"AI: {Player.LeaderName} of the {Player.TribeNamePlural} starts researching {Player.CurrentResearch.Name}.");
		}

		internal void CityProduction(City city)
		{
			if (city == null || city.Size == 0 || city.Tile == null || Player != city.Owner) return;

			IProduction production = null;

			// Create 2 defensive units per city
			if (Player.HasAdvance<LaborUnion>())
			{
				if (city.Tile.Units.Count(x => x is MechInf) < 2) production = new MechInf();
			}
			else if (Player.HasAdvance<Conscription>())
			{
				if (city.Tile.Units.Count(x => x is Riflemen) < 2) production = new Riflemen();
			}
			else if (Player.HasAdvance<Gunpowder>())
			{
				if (city.Tile.Units.Count(x => x is Musketeers) < 2) production = new Musketeers();
			}
			else if (Player.HasAdvance<BronzeWorking>())
			{
				if (city.Tile.Units.Count(x => x is Phalanx) < 2) production = new Phalanx();
			}
			else
			{
				if (city.Tile.Units.Count(x => x is Militia) < 2) production = new Militia();
			}
			
			// Create city improvements
			if (production == null)
			{
				if (!city.HasBuilding<Barracks>()) production = new Barracks();
				else if (Player.HasAdvance<Pottery>() && !city.HasBuilding<Granary>()) production = new Granary();
				else if (Player.HasAdvance<CeremonialBurial>() && !city.HasBuilding<Temple>()) production = new Temple();
				else if (Player.HasAdvance<Masonry>() && !city.HasBuilding<CityWalls>()) production = new CityWalls();
			}

			// Create Settlers
			if (production == null)
			{
				int minCitySize = Leader.Development == DevelopmentLevel.Expansionistic ? 2 : Leader.Development == DevelopmentLevel.Normal ? 3 : 4;
				int maxCities = Leader.Development == DevelopmentLevel.Expansionistic ? 13 : Leader.Development == DevelopmentLevel.Normal ? 10 : 7;
				if (city.Size >= minCitySize && !city.Units.Any(x => x is Settlers) && Player.Cities.Length < maxCities) production = new Settlers();
			}

			// Create some other unit
			if (production == null)
			{
				if (city.Units.Length < 4)
				{
					if (Player.Government is Governments.Republic || Player.Government is Governments.Democracy)
					{
						if (Player.HasAdvance<Writing>()) production = new Diplomat();
					}
					else 
					{
						if (Player.HasAdvance<Automobile>()) production = new Armor();
						else if (Player.HasAdvance<Metallurgy>()) production = new Cannon();
						else if (Player.HasAdvance<Chivalry>()) production = new Knights();
						else if (Player.HasAdvance<TheWheel>()) production = new Chariot();
						else if (Player.HasAdvance<HorsebackRiding>()) production = new Cavalry();
						else if (Player.HasAdvance<IronWorking>()) production = new Legion();
					}
				}
				else
				{
					if (Player.HasAdvance<Trade>()) production = new Caravan();
				}
			}

			// Set random production
			if (production == null)
			{
				IProduction[] items = city.AvailableProduction.ToArray();
				production = items[Common.Random.Next(items.Length)];
			}

			city.SetProduction(production);
		}

		private static Dictionary<Player, AI> _instances = new Dictionary<Player, AI>();
		internal static AI Instance(Player player)
		{
			if (_instances.ContainsKey(player))
				return _instances[player];
			_instances.Add(player, new AI(player));
			return _instances[player];
		}

        // Adapted from darkpanda's civlogic port
        private static City FindNearestCity(int x, int y)
        {
            City nearestCity = null;
            int bestDistance = int.MaxValue;
            foreach (var city in Game.GetCities())
            {
                // TODO fire-eggs: copied from A*
                var dist = Common.Distance(city.X, city.Y, x, y);
                if (dist < bestDistance)
                {
                    bestDistance = dist;
                    nearestCity = city;
                }
            }
            return nearestCity;
        }

        private static bool IsEnemyUnitOrCityNearby(IUnit unit)
        {
            bool isEnemyUnit = IsEnemyUnitNearby(unit);
            var city = FindNearestCity(unit.X, unit.Y);
            if (city == null) 
                return isEnemyUnit;
            if (city.Owner != unit.Owner &&
                Common.Distance(city.X, city.Y, unit.X, unit.Y) == 1)
                return true;
            return isEnemyUnit;
        }

        private static bool IsEnemyUnitNearby(IUnit unit)
        {
            // TODO fire-eggs it is not specified what "nearby" means: assuming distance 1 for now
            int minX = unit.X - 1;
            int maxX = unit.X + 1;
            int minY = unit.Y - 1;
            int maxY = unit.Y + 1;
            var enemies = Game.GetUnits().Where(u => u.X >= minX &&
                                                     u.X <= maxX &&
                                                     u.Y >= minY &&
                                                     u.Y <= maxY &&
                                                     u.Owner != unit.Owner).ToArray();
            return enemies.Length > 0;
        }

        private AI(Player player)
		{
			Player = player;
		}
	}
}