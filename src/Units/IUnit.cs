// CivOne
//
// To the extent possible under law, the person who associated CC0 with
// CivOne has waived all copyright and related or neighboring rights
// to CivOne.
//
// You should have received a copy of the CC0 legalcode along with this
// work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

using System.Collections.Generic;
using System.Drawing;
using CivOne.Advances;
using CivOne.Enums;
using CivOne.Screens;
using CivOne.Tasks;
using CivOne.Tiles;
using CivOne.Units;
using CivOne.UserInterface;
using CivOne.Wonders;

namespace CivOne.Units
{
	public interface IUnit : ICivilopedia, IProduction, ITurn
	{
		IAdvance RequiredTech { get; }
		IWonder RequiredWonder { get; }
		IAdvance ObsoleteTech { get; }
		UnitClass Class { get; }
		/// <summary>
		/// Defines type of the unit
		/// </summary>
		UnitType Type { get; }
		/// <summary>
		/// Defines home of the unit
		/// </summary>
		City Home { get; }
		UnitRole Role { get; }
		byte Attack { get; }
		byte Defense { get; }
		byte Move { get; }
		int X { get; set; }
		int Y { get; set; }
		Point Goto { get; set; }
		/// <summary>
		/// Current tile of `Map` that Unit sit on
		/// </summary>
		ITile Tile { get; }
		/// <summary>
		/// Tells either Unit can move/make its turn or not
		/// </summary>
		bool Busy { get; set; }
		/// <summary>
		/// Unit has Veteran grade
		/// </summary>
		bool Veteran { get; set; }
		/// <summary>
		/// Unit in Sentry mode
		/// </summary>
		bool Sentry { get; set; }
		/// <summary>
		/// Unit got Fortify command
		/// </summary>
		bool FortifyActive { get; }
		/// <summary>
		/// Unit in Fortify mode
		/// </summary>
		bool Fortify { get; set; }
		/// <summary>
		/// Unit is Moving now
		/// </summary>
		bool Moving { get; }
		MoveUnit Movement { get; }
		bool MoveTo(int relX, int relY);
		/// <summary>
		/// Tells who is owner for this Unit
		/// </summary>
		byte Owner { get; set; }
		/// @deprecated
		/// The `Status` / `GetStatus` / `SetStatus` properties/methods were originally written solely
		/// to translate C# state to/from the savefile.
		/// Separation of concerns implies that said code should all be in the savefile handling
		/// modules, and not in the unit classes.
		/// <summary>
		/// The Status property is for saving/restoring state with the savefile
		/// </summary>
		byte Status { get; set; }
		/// <summary>
		/// Current Order for Unit.
		/// Unit can handle only order per turn. Each order can cost some amount of turns.
		/// (See `MovesSkip`)
		/// </summary>
		Order order { get; set; }
		/// <summary>
		/// How many turns Unit should skip to complete the `Order`
		/// </summary>
		int MovesSkip { get; set; }
		/// <summary>
		/// How many turns Unit left to complete the turn
		/// </summary>
		byte MovesLeft { get; set; }
		byte PartMoves { get; set; }
		/// <summary>
		/// Sets `MovesSkip` and completes the turn for Unit
		/// </summary>
		void SkipTurn(int turns = 0);
		IEnumerable<ITile> MoveTargets { get; }
		void Explore();
		/// <summary>
		/// Unit got Home command
		/// </summary>
		void SetHome();
		void SetHome(City city);
		IEnumerable<MenuItem<int>> MenuItems { get; }
		IEnumerable<UnitModification> Modifications { get; }
		/// <summary>
		/// Unit got Pillage command
		/// </summary>
		void Pillage();
	}
}