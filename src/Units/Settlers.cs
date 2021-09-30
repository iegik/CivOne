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
		public override bool Busy
		{
			get
			{
				return (base.Busy || MovesSkip > 0);
			}
			set
			{
				base.Busy = false;
				MovesSkip = 0;
			}
		}
		public Order _order;
		internal void SetStatus(bool[] bits)
		{
			if (Owner == 0)
			{
				return;
			}
			bool cheatEnabled = Human == Owner && Settings.Instance.AutoSettlers; // cheat for human
			if (bits[1] && !bits[6] && !bits[7])
			{
				_order = Order.Road;
				MovesSkip = cheatEnabled ? 1 : 2;
			}
			if (!bits[1] && bits[6] && !bits[7])
			{
				_order = Order.Irrigate;
				MovesSkip = cheatEnabled ? 1 : 3;
			}
			if (!bits[1] && !bits[6] && bits[7])
			{
				_order = Order.Mines;
				MovesSkip = cheatEnabled ? 1 : 4;
			}
			if (!bits[1] && bits[6] && bits[7])
			{
				_order = Order.Fortress;
				MovesSkip = cheatEnabled ? 1 : 5;
			}
            if (bits[1] && !bits[6] && bits[7])
			{
				_order = Order.Pillage;
				MovesSkip = cheatEnabled ? 1 : 5; // TODO verify pollution cost
			}
			MovesLeft = 0;
			PartMoves = 0;
        }

        internal void GetStatus(ref byte result)
        {
            // translate internal state to save file format
			switch (_order) {
				case Order.Road:
					result |= 0x2;
					break;
				case Order.Irrigate:
					result |= 0x40;
					break;
            	case Order.Mines:
					result |= 0x80;
					break;
            	case Order.Fortress:
					result |= 0xc0;
					break;
            	case Order.Pillage:
					result |= 0x82;
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
				return true;
			}
			if (Game.CurrentPlayer.HasAdvance<RailRoad>() && !tile.IsOcean && tile.Road && !tile.RailRoad && tile.City == null)
			{
                Status = 2;
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
                // TODO fire-eggs setting MovesSkip to true should clear moves
                Status = 64;
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
//                    MovesSkip = (Human == Owner && Settings.Instance.AutoSettlers) ? 1 : 3; // cheat for human
//                    MovesLeft = 0;
//					PartMoves = 0;
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
				return true;
			}
			return false;
		}

		public override void NewTurn()
		{
			base.NewTurn();
			if (MovesSkip > 0)
			{
				MovesSkip--;
				MovesLeft = 0;
				PartMoves = 0;
				return;
			}
			if (_order == Order.Road)
			{
				if (Map[X, Y].Road)
				{
					if (Human.HasAdvance<RailRoad>()) // TODO is this 'Human' or 'Owner'?
					{
						Map[X, Y].RailRoad = true;
					}
					foreach (Settlers settlers in Map[X, Y].Units.Where(u => (u is Settlers) && (u as Settlers)._order == Order.Road).Select(u => (u as Settlers)))
					{
						settlers.MovesSkip = 0;
					}
				}
				else
				{
					Map[X, Y].Road = true;
				}
				MovesLeft = 1;
				PartMoves = 0;
			}
			else if (_order == Order.Irrigate)
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
				MovesLeft = 1;
				PartMoves = 0;
			}
			else if (_order == Order.Mines)
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
				MovesLeft = 1;
				PartMoves = 0;
			}
			else if (_order == Order.Fortress)
			{
				Map[X, Y].Fortress = true;
				MovesLeft = 1;
				PartMoves = 0;
			}
			_order = Order.None;
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