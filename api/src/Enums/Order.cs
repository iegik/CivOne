namespace CivOne.Enums
{
	public enum Order
	{
		None,
		NewCity,
		Sentry, // 0x1 0b00000001
		Fortify,
		Road, // 0x2 0b00000010
		Irrigate, // 0x40 0b01000000
		Mines, // 0x80 0b10000000
		Fortress, // 0xc0 0b11000000
		Wait,
		Skip,
		Unload,
		Disband,
		Pillage, // 0x82 0b10000010
		ClearPollution,
	}
}
