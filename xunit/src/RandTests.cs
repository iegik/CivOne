using System;
using System.Collections.Generic;
using System.Text;
using Xunit;

namespace CivOne.UnitTests
{
    // TODO run with more than one initial seed
    public class RandTests
    {
        [Fact]
        public void Percent50()
        {
            Random r = new Random(945);
            int [] counts = new int[3];
            for (int i = 0; i < 10000; i++)
            {
                var val = r.Next(0, 2);
                counts[val]++;
            }

            Assert.Equal(0, counts[2]);
            var val1 = counts[0] / 10000;
            var val2 = counts[1] / 10000;
            Assert.InRange(Math.Abs(counts[0]-counts[1]),0,100);
        }

        [Fact]
        public void Percent33()
        {
            Random r = new Random(945);
            int [] counts = new int[4];
            for (int i = 0; i < 10000; i++)
            {
                var val = r.Next(0, 3);
                counts[val]++;
            }

            Assert.Equal(0, counts[3]);
            Assert.InRange(Math.Abs(counts[0]-counts[1]),0,75);
            Assert.InRange(Math.Abs(counts[1]-counts[2]),0,75);
        }
    }
}
