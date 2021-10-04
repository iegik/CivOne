// CivOne
//
// To the extent possible under law, the person who associated CC0 with
// CivOne has waived all copyright and related or neighboring rights
// to CivOne.
//
// You should have received a copy of the CC0 legalcode along with this
// work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

using CivOne.Advances;
using CivOne.Enums;
using CivOne.IO;
using CivOne.Tasks;
using CivOne.Tiles;
using CivOne.UserInterface;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;

namespace CivOne.Units
{
    internal class Settlers : BaseUnitLand
	{
		internal void SetStatus(bool[] bits)
		{
			if (bits[1] && !bits[6] && !bits[7])
			{
				order = Order.Road;
			}
			if (!bits[1] && bits[6] && !bits[7])
			{
				order = Order.Irrigate;
			}
			if (!bits[1] && !bits[6] && bits[7])
			{
				order = Order.Mines;
			}
			if (!bits[1] && bits[6] && bits[7])
			{
				order = Order.Fortress;
			}
            if (bits[1] && !bits[6] && bits[7])
			{
				order = Order.Pillage;
			}
        }

        internal void GetStatus(ref byte result)
        {
            // translate internal state to save file format
			switch (order) {
				case Order.Road:
					result |= 0b00000010;
					break;
				case Order.Irrigate:
					result |= 0b01000000;
					break;
            	case Order.Mines:
					result |= 0b10000000;
					break;
            	case Order.Fortress:
					result |= 0b11000000;
					break;
            	case Order.Pillage:
					result |= 0b10000010;
					break;
			}
        }

		public bool BuildRoad()
		{
			ITile tile = Map[X, Y];
			if (tile.RailRoad)
			{
                // TODO attempt to double-build road?
				// There is already a RailRoad here, don't build another one
				return false;
			}
			if (!tile.IsOcean && !tile.Road && tile.City == null)
			{
				if ((tile is River) && !Game.CurrentPlayer.HasAdvance<BridgeBuilding>())
					return false;
                Status = 2;
				SkipTurn(2);
				return true;
			}
			if (Game.CurrentPlayer.HasAdvance<RailRoad>() && !tile.IsOcean && tile.Road && !tile.RailRoad && tile.City == null)
			{
                Status = 2;
				SkipTurn(3);
				return true;
			}
			return false;
		}

		public bool BuildIrrigation()
		{
			ITile tile = Map[X, Y];
			if (tile.Irrigation || tile.IsOcean) // already irrigated or illogical: ignore
			{
				return false;
			}

            // Changing terrain type
			if (tile.IrrigationChangesTerrain())
			{
                Status = 64;
				SkipTurn(4);
				return true;
			}

            //bool irrigate1 = (tile.GetBorderTiles().Any(t => (t.X == X || t.Y == Y) &&
            //                                                 (t.City == null) &&
            //                                                 (t.IsOcean || t.Irrigation || (t is River)))) ||
            //                 (tile is River);
            //bool irrigate1a =
            //    tile.CrossTiles().Any(t => !t.HasCity && (t.IsOcean || t.Irrigation || t.Type == Terrain.River)) ||
            //    tile.Type == Terrain.River;

            //Debug.Assert(irrigate1 == irrigate1a);

            //bool irrigate2 = tile.AllowIrrigation();
            //bool irrigate2a = tile.AllowIrrigation() || tile.Type == Terrain.River;
            //Debug.Assert(irrigate2a == irrigate1);

            //bool irrigate3 = !tile.IsOcean &&       // always false
            //                 !(tile.Irrigation) &&  // always false
            //                 ((tile is Desert) ||
            //                  (tile is Grassland) ||
            //                  (tile is Hills) ||
            //                  (tile is Plains) ||
            //                  (tile is River));

            //bool irrigate4 =
            //    (((tile is Desert) || (tile is Grassland) || (tile is Hills) || (tile is Plains) || (tile is River)) &&
            //     tile.City == null);
            //Debug.Assert(irrigate4 == irrigate3);

            if (!tile.TerrainAllowsIrrigation())
            {
                if (Human == Owner)
                    GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
                return false;
            }

            if (tile.AllowIrrigation() || tile.Type == Terrain.River)
            {
                Status = 64;
				SkipTurn(3);
                return true;
            }

            if (Human == Owner)
                GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
            return false;

////			if ((tile.GetBorderTiles().Any(t => (t.X == X || t.Y == Y) && (t.City == null) && (t.IsOcean || t.Irrigation || (t is River)))) || (tile is River))
//            if (tile.AllowIrrigation() || tile.Type == Terrain.River) // source of water for irrigation available
//            {
//				//if (!tile.IsOcean && !(tile.Irrigation) && ((tile is Desert) || (tile is Grassland) || (tile is Hills) || (tile is Plains) || (tile is River)))
//                if (tile.TerrainAllowsIrrigation()) // irrigation may be applied
//				{
//					MovesSkip = (Human == Owner && Settings.Instance.AutoSettlers) ? 1 : 3; // cheat for human
//					SkipTurn();
//					return true;
//				}
//				if (Human == Owner)
//					GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
//				return false;
//			}

//			{
//				if (((tile is Desert) || (tile is Grassland) || (tile is Hills) || (tile is Plains) || (tile is River)) && tile.City == null)
//				{
//					if (Human == Owner)
//						GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOWATER")));
//					return false;
//				}
//				if (Human == Owner)
//					GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
//			}
//			return false;
		}

		public bool BuildMines()
		{
			ITile tile = Map[X, Y];
			if (!tile.IsOcean && !(tile.Mine) && ((tile is Desert) || (tile is Hills) || (tile is Mountains) || (tile is Jungle) || (tile is Grassland) || (tile is Plains) || (tile is Swamp)))
			{
                Status = 128;
				SkipTurn(4);
				return true;
			}
			return false;
		}

		public bool BuildFortress()
		{
			if (!Game.CurrentPlayer.HasAdvance<Construction>())
				return false;

			ITile tile = Map[X, Y];
			if (!tile.IsOcean && !(tile.Fortress) && tile.City == null)
			{
                Status = 0xc0;
				SkipTurn(5);
				return true;
			}
			return false;
		}

		public override void ExecuteOrder()
		{
			base.ExecuteOrder();
			if (MovesSkip > 0)
			{
				return;
			}

			if (order == Order.Road)
			{
				if (Map[X, Y].Road)
				{
					if (Human.HasAdvance<RailRoad>()) // TODO is this 'Human' or 'Owner'?
					{
						Map[X, Y].RailRoad = true;
					}
					foreach (Settlers settlers in Map[X, Y].Units.Where(u => (u is Settlers) && (u as Settlers).order == Order.Road).Select(u => (u as Settlers)))
					{
						settlers.MovesSkip = 0;
					}
				}
				else
				{
					Map[X, Y].Road = true;
				}
				order = Order.None;
			}
			else if (order == Order.Irrigate)
			{
				Map[X, Y].Irrigation = false;
				Map[X, Y].Mine = false;
				if (Map[X, Y] is Forest)
				{
					Map.ChangeTileType(X, Y, Terrain.Plains);
				}
				else if ((Map[X, Y] is Jungle) || (Map[X, Y] is Swamp))
				{
					Map.ChangeTileType(X, Y, Terrain.Grassland1);
				}
				else
				{
					Map[X, Y].Irrigation = true;
				}
				order = Order.None;
			}
			else if (order == Order.Mines)
			{
				if ((Map[X, Y] is Jungle) || (Map[X, Y] is Grassland) || (Map[X, Y] is Plains) || (Map[X, Y] is Swamp))
				{
					Map[X, Y].Irrigation = false;
					Map[X, Y].Mine = false;
					Map.ChangeTileType(X, Y, Terrain.Forest);
				}
				else
				{
					Map[X, Y].Irrigation = false;
					Map[X, Y].Mine = true;
				}
				order = Order.None;
			}
			else if (order == Order.Fortress)
			{
				Map[X, Y].Fortress = true;
				order = Order.None;
			}
        }

		private MenuItem<int> MenuFoundCity() => MenuItem<int>
			.Create((Map[X, Y].City == null) ? "Found New City" : "Add to City")
			.SetShortcut("b")
			.OnSelect((s, a) => GameTask.Enqueue(Orders.FoundCity(this)));

		private MenuItem<int> MenuBuildRoad() => MenuItem<int>
			.Create((Map[X, Y].Road) ? "Build RailRoad" : "Build Road")
			.SetShortcut("r")
			.OnSelect((s, a) => BuildRoad());

		private MenuItem<int> MenuBuildIrrigation() => MenuItem<int>
			.Create((Map[X, Y] is Forest) ? "Change to Plains" :
					((Map[X, Y] is Jungle) || (Map[X, Y] is Swamp)) ? "Change to Grassland" :
					"Build Irrigation")
			.SetShortcut("i")
			.SetEnabled(Map[X, Y].AllowIrrigation() || Map[X, Y].IrrigationChangesTerrain())
			.OnSelect((s, a) => GameTask.Enqueue(Orders.BuildIrrigation(this)));

		private MenuItem<int> MenuBuildMines() => MenuItem<int>
			.Create(((Map[X, Y] is Jungle) || (Map[X, Y] is Grassland) || (Map[X, Y] is Plains) || (Map[X, Y] is Swamp)) ?
					"Change to Forest" : "Build Mines")
			.SetShortcut("m")
			.OnSelect((s, a) => GameTask.Enqueue(Orders.BuildMines(this)));

		private MenuItem<int> MenuBuildFortress() => MenuItem<int>
			.Create("Build fortress")
			.SetShortcut("f")
			.SetEnabled(Game.CurrentPlayer.HasAdvance<Construction>())
			.OnSelect((s, a) => GameTask.Enqueue(Orders.BuildFortress(this)));

		public override IEnumerable<MenuItem<int>> MenuItems
		{
			get
			{
				ITile tile = Map[X, Y];

				yield return MenuNoOrders();
				if (!tile.IsOcean)
				{
					yield return MenuFoundCity();
				}
				if (!tile.IsOcean && (!tile.Road || (Human.HasAdvance<RailRoad>() && !tile.RailRoad)))
				{
					yield return MenuBuildRoad();
				}
				if (!tile.Irrigation && (tile.TerrainAllowsIrrigation() || tile.IrrigationChangesTerrain())) // ((tile is Desert) || (tile is Grassland) || (tile is Hills) || (tile is Plains) || (tile is River) || (tile is Forest) || (tile is Jungle) || (tile is Swamp)))
				{
					yield return MenuBuildIrrigation();
				}
				if (!tile.Mine && ((tile is Desert) || (tile is Hills) || (tile is Mountains) || (tile is Jungle) || (tile is Grassland) || (tile is Plains) || (tile is Swamp)))
				{
					yield return MenuBuildMines();
				}
				if (!tile.IsOcean && !tile.Fortress)
				{
					yield return MenuBuildFortress();
				}
				//
				yield return MenuWait();
				yield return MenuSentry();
				yield return MenuGoTo();
				if (tile.Irrigation || tile.Mine || tile.Road || tile.RailRoad)
				{
					yield return MenuPillage();
				}
				if (tile.City != null)
				{
					yield return MenuHomeCity();
				}
				yield return null;
				yield return MenuDisbandUnit();
			}
		}

		public override void SkipTurn(int turns = 0)
		{
			MovesSkip = turns;
			bool cheatEnabled = Human == Owner && Settings.Instance.AutoSettlers; // cheat for human
			if (turns > 1 && cheatEnabled) MovesSkip = 1;
			MovesLeft = 0;
			PartMoves = 0;
		}

		public Settlers() : base(4, 0, 1, 1)
		{
			Type = UnitType.Settlers;
			Name = "Settlers";
			RequiredTech = null;
			ObsoleteTech = null;
			SetIcon('D', 1, 1);
            Role = UnitRole.Settler;
        }
	}
}