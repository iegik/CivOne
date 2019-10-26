using System.Linq;
using CivOne.Enums;
using CivOne.Tiles;
using CivOne.Units;
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

        [Fact]
        public void ZOKRevertTest1()
        {
            // Fixing Issue #93, I reverted the situation where the player's unit should be
            // able to move to a space with it's own unit

            // find another player
            var otherP = Game.Instance.Players.First(p => p.Civilization.Name != "Chinese");

            // give other player a city
            Game.Instance.AddCity(otherP, 3, 52, 14);
            Assert.Equal(true, Map.Instance[52,14].HasCity);

            // set up other player unit in city
            Game.Instance.CreateUnit(UnitType.Militia, 52, 14, Game.Instance.PlayerNumber(otherP));

            // give human two units
            var chariot1 = Game.Instance.CreateUnit(UnitType.Chariot, 53, 14, Game.Instance.PlayerNumber(playa));
            var chariot2 = Game.Instance.CreateUnit(UnitType.Chariot, 52, 15, Game.Instance.PlayerNumber(playa));

            // try to move the human unit down and left to own unit
            var gm = new Screens.GamePlayPanels.GameMap();
            Game.Instance._currentPlayer = Game.Instance.PlayerNumber(playa);
            Game.Instance.ActiveUnit = chariot1;
            Assert.True(gm.MoveTo(-1, +1));
        }
        [Fact]
        public void ZOKRevertTest2()
        {
            // Confirm that the player's unit should be able to move to a space with it's own city,
            // even when "blocked" by enemy unit: player unit should be able to move down one space below:
            /*
              | enemy |player|
              | city  | unit |
              --------+------+-------
              |       |player| 
              |       | city | 
              +-------+------+-------
              |       |      |  
              |       |      |
             */

            // find another player
            var otherP = Game.Instance.Players.First(p => p.Civilization.Name != "Chinese");

            // give other player a unit
            Game.Instance.CreateUnit(UnitType.Militia, 51, 13, Game.Instance.PlayerNumber(otherP));

            // give human a city
            Game.Instance.AddCity(playa, 3, 52, 14);
            Assert.Equal(true, Map.Instance[52,14].HasCity);

            // give human a unit
            var chariot1 = Game.Instance.CreateUnit(UnitType.Chariot, 52, 13, Game.Instance.PlayerNumber(playa));

            // try to move the human unit down to own city
            var gm = new Screens.GamePlayPanels.GameMap();
            Game.Instance._currentPlayer = Game.Instance.PlayerNumber(playa);
            Game.Instance.ActiveUnit = chariot1;
            Assert.True(gm.MoveTo(0, +1));
        }
        [Fact]
        public void ZOKRevertTest3()
        {
            // find another player
            var otherP = Game.Instance.Players.First(p => p.Civilization.Name != "Chinese");

            // give other player a city [undefended]
            Game.Instance.AddCity(otherP, 3, 52, 14);
            Assert.Equal(true, Map.Instance[52,14].HasCity);

            // give other player a unit
            Game.Instance.CreateUnit(UnitType.Militia, 51, 13, Game.Instance.PlayerNumber(otherP));

            // give human a unit
            var chariot1 = Game.Instance.CreateUnit(UnitType.Chariot, 52, 13, Game.Instance.PlayerNumber(playa));

            // try to move the human unit down to enemy city
            Game.Instance._currentPlayer = Game.Instance.PlayerNumber(playa);
            Game.Instance.ActiveUnit = chariot1;
            Assert.True(((BaseUnit)chariot1).CanMoveTo(0, +1));
        }
    }
}