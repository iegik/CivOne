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
using System.Linq;

namespace CivOne.Units
{
    internal class Settlers : BaseUnitLand
	{
		internal void SetStatus(bool[] bits)
		{
			// TODO initialize MovesSkip value
			// TODO need to set MovesSkip from savefile format [see TODO in GetStatus]
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
				order = Order.ClearPollution;
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
				// TODO in classic CIV, allowed to build road on Ocean!

				if ((tile is River) && !Game.CurrentPlayer.HasAdvance<BridgeBuilding>())
					return false;
				order = Order.Road;
				SkipTurn(tile.RoadCost);
				return true;
			}
			if (Game.CurrentPlayer.HasAdvance<RailRoad>() && !tile.IsOcean && tile.Road && !tile.RailRoad && tile.City == null)
			{
				// TODO in classic CIV, allowed to build railroad on Ocean!

				order = Order.Road;
				SkipTurn(tile.RailRoadCost);
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
				order = Order.Irrigate;
				SkipTurn(tile.IrrigationCost);
				return true;
			}

            if (!tile.TerrainAllowsIrrigation())
            {
                if (Human == Owner)
                    GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
                return false;
            }

            if (tile.AllowIrrigation() || tile.Type == Terrain.River)
            {
				order = Order.Irrigate;
				SkipTurn(tile.IrrigationCost);
                return true;
            }

            if (Human == Owner)
                GameTask.Enqueue(Message.Error("-- Civilization Note --", TextFile.Instance.GetGameText("ERROR/NOIRR")));
            return false;
		}

		public bool BuildMines()
		{
			ITile tile = Map[X, Y];
			if (!tile.IsOcean && !(tile.Mine) && ((tile is Desert) || (tile is Hills) || (tile is Mountains) || (tile is Jungle) || (tile is Grassland) || (tile is Plains) || (tile is Swamp)))
			{
				order = Order.Mines;
				SkipTurn(tile.MiningCost);
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
				order = Order.Fortress;
				SkipTurn(5); // TODO IUnit.FortressCost?
				return true;
			}
			return false;
		}

		public override void NewTurn()
		{
			base.NewTurn();
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
					// TODO why is this done for railroad but not road/irrigate/mine/etc?
					foreach (Settlers settlers in Map[X, Y].Units.Where(u => (u is Settlers) && (u as Settlers).order == Order.Road).Select(u => (u as Settlers)))
					{
						settlers.order = Order.None;
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
					// TODO classic CIV allowed building road/railroad on ocean
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

		/// <summary>
		/// A settlers-specific version of SkipTurn to manage MovesSkip
		/// </summary>
		/// <param name="turns">The number of turns to set MovesSkip</param>
		public void SkipTurn(int turns = 0)
		{
			base.SkipTurn();
			MovesSkip = turns;
			bool cheatEnabled = Human == Owner && Settings.Instance.AutoSettlers; // cheat for human
			if (turns > 1 && cheatEnabled) MovesSkip = 1;
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