using System;
using System.Linq;
using System.Threading;
using CivOne.UnitTests;

namespace CivOne.src
{
    /// <summary>
    /// All tests need to derive from this class. A fresh runtime, map, game, and player
    /// are built up and torn down for each test.
    /// </summary>
    public abstract class TestsBase : IDisposable
    {
        private RuntimeSettings rs;
        private MockRuntime runtime;
        internal Player playa;

        /// <summary>
        /// A hard-coded Game, using Earth and the player as Babylonian.
        /// </summary>
        protected TestsBase()
        {
            // TODO fire-eggs does this work to create the same "game" each time?
            var r = new Random(23905);

            rs = new RuntimeSettings();
            runtime = new MockRuntime(rs);

            // Load Earth map
            var foo = Map.Instance;
            foo.LoadMap();
            do
            {
                Thread.Sleep(5);
            } while (!foo.Ready);

            // Start with Babylonians at King level
            Game.CreateGame(3, 2, Common.Civilizations.First(x => x.Name=="Babylonian"));
            playa = Game.Instance.HumanPlayer;
        }

        public void Dispose()
        {
            // Tear everything down
            Map.Wipe();
            Game.Wipe();
            runtime?.Dispose();
            RuntimeHandler.Wipe();
            GC.Collect();
        }
    }
}
