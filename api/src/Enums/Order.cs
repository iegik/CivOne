namespace CivOne.Enums
{
	public enum Order
	{
		None,
		NewCity,
		Sentry,
		Fortify,
		Road, // 0x2
		Irrigate, // 0x40
		Mines, // 0x80
		Fortress, // 0xc0
		Wait,
		Skip,
		Unload,
		Disband,
		Pillage // 0x82
	}
}
