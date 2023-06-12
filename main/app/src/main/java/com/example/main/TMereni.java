package com.example.main;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.Date;
import java.time.Instant;
import com.example.main.PacketType;

public class TMereni {
    public String Serial;
    public PacketType packetType;   // typ packetu, 1..datum, 2..data
    public int hh, mm, ss;    // hodiny, minuty
    public int year;
    public int month, day;   // rok/mesic/den
    public int gtm;         // posun gtm
    public int adc;
    public int hum;
    public double t1, t2, t3;
    public int Err;
    public int mvs;
    public Date dtm;

}
