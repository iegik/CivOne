using System.Linq;
using CivOne.Enums;
using CivOne.Screens;
using CivOne.Tiles;
using Xunit;

namespace CivOne.UnitTests
{
    public class ZOCTests : TestsBase2
    {
        // TODO game created in base is NOT always consistent in players!  WHY???

        private bool TestZoc(bool defendedCity)
        {
            /* Establish the scenario described in Issue #93:
              | enemy |      |
              | city  |      |
              --------+------+-------
              |       |      | enemy
              |       |      |  unit
              +-------+------+-------
              |       |player|  
              |       | unit |
              The player unit wants to try to move up & left.

              This is OK if the enemy city is undefended.
              This is not OK if the enemy city is defended.
            */
            // find another player
            var otherP = Game.Instance.Players.First(p => p.Civilization.Name != "Chinese");

            // give other player a city
            Game.Instance.AddCity(otherP, 3, 52, 14);
            Assert.Equal(true, Map.Instance[52,14].HasCity);

            // set up other player units
            if (defendedCity)
                Game.Instance.CreateUnit(UnitType.Militia, 52, 14, Game.Instance.PlayerNumber(otherP));
            Game.Instance.CreateUnit(UnitType.Militia,54,15, Game.Instance.PlayerNumber(otherP));

            // give human a unit
            var chariot = Game.Instance.CreateUnit(UnitType.Chariot, 53, 16, Game.Instance.PlayerNumber(playa));

            // try to move the human unit up and left
            var gm = new Screens.GamePlayPanels.GameMap();
            Game.Instance._currentPlayer = Game.Instance.PlayerNumber(playa);
            Game.Instance.ActiveUnit = chariot;
            return gm.MoveTo(-1, -1);
        }

        [Fact]
        public void ZOCFixTest1()
        {
            // Test bugfix for issue #93. SWY's code failed this.

            Assert.False(TestZoc(true));
        }

        [Fact]
        public void ZOCFixTest2()
        {
            // Test bugfix for issue #93: insure the opposite case isn't broken

            Assert.True(TestZoc(false));
        }

        public void AllowIrrigate()
        {
            // Test bugfix: AllowIrrigation() would return true for 
            // a tile immediately west of a city.

            // Using seed of 7595, Earth, Chinese, the initial city is
            // like so:
            //  P P O  - P: plains, O: ocean, M: Mtns, C: city
            //  P C O
            //  M P P

            var unit = Game.Instance.GetUnits().First(x => playa == x.Owner);
            City acity = Game.Instance.AddCity(playa, 1, unit.X, unit.Y);

            ITile tile = Map.Instance[unit.X, unit.Y];
            Assert.Equal(true, tile.HasCity);
            Assert.True(tile is Grassland);

            // Before 20190810, this would incorrectly return true
            Assert.False(Map.Instance[unit.X-1, unit.Y].AllowIrrigation());

            Assert.False(Map.Instance[unit.X-1, unit.Y-1].AllowIrrigation());
            Assert.False(Map.Instance[unit.X-1, unit.Y+1].AllowIrrigation());
            Assert.False(Map.Instance[unit.X-0, unit.Y+1].AllowIrrigation());

            Assert.True(Map.Instance[unit.X-0, unit.Y-1].AllowIrrigation());
            Assert.True(Map.Instance[unit.X+1, unit.Y+1].AllowIrrigation());
        }
    }
}