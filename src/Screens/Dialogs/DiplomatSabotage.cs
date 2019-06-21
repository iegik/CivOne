// CivOne
//
// To the extent possible under law, the person who associated CC0 with
// CivOne has waived all copyright and related or neighboring rights
// to CivOne.
//
// You should have received a copy of the CC0 legalcode along with this
// work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

using System;
using System.Linq;
using CivOne.Graphics;
using CivOne.Tiles;
using CivOne.Units;
using CivOne.UserInterface;
using CivOne.Buildings;
using CivOne.Tasks;
using System.Collections.Generic;

namespace CivOne.Screens.Dialogs
{
	internal class DiplomatSabotage : BaseDialog
	{
		private const int FONT_ID = 0;

        internal DiplomatSabotage(City enemyCity, Diplomat diplomat) : base(60, 80, 175, 56)
		{
            var enemyCity1 = enemyCity ?? throw new ArgumentNullException(nameof(enemyCity));
			var diplomat1 = diplomat ?? throw new ArgumentNullException(nameof(diplomat));

			IBitmap spyPortrait = Icons.Spy;

			Palette palette = Common.DefaultPalette;
			for (int i = 144; i < 256; i++)
			{
				palette[i] = spyPortrait.Palette[i];
			}
			this.SetPalette(palette);

			DialogBox.AddLayer(spyPortrait, 2, 2);

			DialogBox.DrawText($"Spies Report", 0, 15, 45, 5);
			DialogBox.DrawText(diplomat1.Sabotage(enemyCity1), 0, 15, 45, 5 + Resources.GetFontHeight(FONT_ID));
			DialogBox.DrawText($"in {enemyCity1.Name}", 0, 15, 45, 5 + (2 * Resources.GetFontHeight(FONT_ID)));

            // TODO KBR set width based on text
		}
	}
}