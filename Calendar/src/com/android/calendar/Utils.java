/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import static android.provider.Calendar.EVENT_BEGIN_TIME;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.ViewFlipper;
// For Lunar Calendar calculation ...
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class Utils {
    private static final int CLEAR_ALPHA_MASK = 0x00FFFFFF;
    private static final int HIGH_ALPHA = 255 << 24;
    private static final int MED_ALPHA = 180 << 24;
    private static final int LOW_ALPHA = 150 << 24;
	
    public static class lunarDateInfo {
		public int lYear;
		public int lMonth;
		public int lDay;
		public boolean leap = false;
		public boolean lessSpanFlag = false;

	}

	private static final String TAG = "CalendarUtils";
	
    public static class lunarInfo {
		/**
		 * Provides functions to calculate Chinese lunar calendar date based on
		 *   western calendar.
		 * FIXME: Should it be in native code?
		 */
		public String lunarStr;
		public int lunarType;
	    static final int LUNAR_TYPE_NORMAL = 0;
	    static final int LUNAR_TYPE_SOLAR_TEAM = 1;
	    static final int LUNAR_TYPE_FESTIVAL = 2;

		public lunarInfo() {
		}
	}

    protected static final String OPEN_EMAIL_MARKER = " <";
    protected static final String CLOSE_EMAIL_MARKER = ">";

    /* The corner should be rounded on the top right and bottom right */
    private static final float[] CORNERS = new float[] {0, 0, 5, 5, 5, 5, 0, 0};


    public static void startActivity(Context context, String className, long time) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setClassName(context, className);
        intent.putExtra(EVENT_BEGIN_TIME, time);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(intent);
    }

    static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    static void setDefaultView(Context context, int viewId) {
        String activityString = CalendarApplication.ACTIVITY_NAMES[viewId];

        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        if (viewId == CalendarApplication.AGENDA_VIEW_ID ||
                viewId == CalendarApplication.DAY_VIEW_ID) {
            // Record the (new) detail start view only for Agenda and Day
            editor.putString(CalendarPreferenceActivity.KEY_DETAILED_VIEW, activityString);
        }

        // Record the (new) start view
        editor.putString(CalendarPreferenceActivity.KEY_START_VIEW, activityString);
        editor.commit();
    }

    public static final Time timeFromIntent(Intent intent) {
        Time time = new Time();
        time.set(timeFromIntentInMillis(intent));
        return time;
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        MatrixCursor newCursor = new MatrixCursor(cursor.getColumnNames());
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    /**
     * Compares two cursors to see if they contain the same data.
     *
     * @return Returns true of the cursors contain the same data and are not null, false
     * otherwise
     */
    public static boolean compareCursors(Cursor c1, Cursor c2) {
        if(c1 == null || c2 == null) {
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while(c1.moveToNext() && c2.moveToNext()) {
            for(int i = 0; i < numColumns; i++) {
                if(!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that.  Otherwise, use the current time.
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if(path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time " +
                            "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    public static final void applyAlphaAnimation(ViewFlipper v) {
        AlphaAnimation in = new AlphaAnimation(0.0f, 1.0f);

        in.setStartOffset(0);
        in.setDuration(500);

        AlphaAnimation out = new AlphaAnimation(1.0f, 0.0f);

        out.setStartOffset(0);
        out.setDuration(500);

        v.setInAnimation(in);
        v.setOutAnimation(out);
    }

    public static Drawable getColorChip(int color) {
        /*
         * We want the color chip to have a nice gradient using
         * the color of the calendar. To do this we use a GradientDrawable.
         * The color supplied has an alpha of FF so we first do:
         * color & 0x00FFFFFF
         * to clear the alpha. Then we add our alpha to it.
         * We use 3 colors to get a step effect where it starts off very
         * light and quickly becomes dark and then a slow transition to
         * be even darker.
         */
        color &= CLEAR_ALPHA_MASK;
        int startColor = color | HIGH_ALPHA;
        int middleColor = color | MED_ALPHA;
        int endColor = color | LOW_ALPHA;
        int[] colors = new int[] {startColor, middleColor, endColor};
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        d.setCornerRadii(CORNERS);
        return d;
    }

    /**
     * Formats the given Time object so that it gives the month and year
     * (for example, "September 2007").
     *
     * @param time the time to format
     * @return the string containing the weekday and the date
     */
    public static String formatMonthYear(Context context, Time time) {
        return time.format(context.getResources().getString(R.string.month_year));
    }
    
    /**
     * Get first day of week as android.text.format.Time constant.
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek() {
        int startDay = Calendar.getInstance().getFirstDayOfWeek();
        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }

    /**
     * Determine whether the column position is Saturday or not.
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Saturday position
     */
    public static boolean isSaturday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 6)
            || (firstDayOfWeek == Time.MONDAY && column == 5)
            || (firstDayOfWeek == Time.SATURDAY && column == 0);
    }

    /**
     * Determine whether the column position is Sunday or not.
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Sunday position
     */
    public static boolean isSunday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 0)
            || (firstDayOfWeek == Time.MONDAY && column == 6)
            || (firstDayOfWeek == Time.SATURDAY && column == 1);
    }

    /**
     * Scan through a cursor of calendars and check if names are duplicated.
     *
     * This travels a cursor containing calendar display names and fills in the provided map with
     * whether or not each name is repeated.
     * @param isDuplicateName The map to put the duplicate check results in.
     * @param cursor The query of calendars to check
     * @param nameIndex The column of the query that contains the display name
     */
    public static void checkForDuplicateNames(Map<String, Boolean> isDuplicateName, Cursor cursor,
            int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            // Set it to true if we've seen this name before, false otherwise
            if (displayName != null) {
                isDuplicateName.put(displayName, isDuplicateName.containsKey(displayName));
            }
        }
    }

    // TODO: replace this with the correct i18n way to do this
    public static final String englishNthDay[] = {
        "", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
        "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th",
        "20th", "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th",
        "30th", "31st"
    };

    public static String formatNth(int nth) {
        return "the " + englishNthDay[nth];
    }

    /**
     * Sets the time to the beginning of the day (midnight) by clearing the
     * hour, minute, and second fields.
     */
    static void setTimeToStartOfDay(Time time) {
        time.second = 0;
        time.minute = 0;
        time.hour = 0;
    }

    //// e2437c
    // BELOW are for lunar calendar, and probably should be moved to native Time tool..
    //
    final private static int LUNAR_START_YEAR = 1901;
    final private static int LUNAR_END_YEAR = 2100;

    // gLunarMonthDay stores days information from 1901 to 2100.
    //   In Lunar Calendar, each month has 29 days or 30 days, each year has 12
    //   or 13 months. Here uses higher 12 or 13 bits to represent each month
    //   in a year, 1: 30 days, 0: 29 days.
    final private static int gLunarMonthDay[] =
    {
        0X4ae0, 0Xa570, 0X5268, 0Xd260, 0Xd950, 0X6aa8, 0X56a0, 0X9ad0, 0X4ae8, 0X4ae0,   //1901-1910
        0Xa4d8, 0Xa4d0, 0Xd250, 0Xd528, 0Xb540, 0Xd6a0, 0X96d0, 0X95b0, 0X49b8, 0X4970,   //1911-1920
        0Xa4b0, 0Xb258, 0X6a50, 0X6d40, 0Xada8, 0X2b60, 0X9570, 0X4978, 0X4970, 0X64b0,   //1921-1930
        0Xd4a0, 0Xea50, 0X6d48, 0X5ad0, 0X2b60, 0X9370, 0X92e0, 0Xc968, 0Xc950, 0Xd4a0,   //1931-1940
        0Xda50, 0Xb550, 0X56a0, 0Xaad8, 0X25d0, 0X92d0, 0Xc958, 0Xa950, 0Xb4a8, 0X6ca0,   //1941-1950
        0Xb550, 0X55a8, 0X4da0, 0Xa5b0, 0X52b8, 0X52b0, 0Xa950, 0Xe950, 0X6aa0, 0Xad50,   //1951-1960
        0Xab50, 0X4b60, 0Xa570, 0Xa570, 0X5260, 0Xe930, 0Xd950, 0X5aa8, 0X56a0, 0X96d0,   //1961-1970
        0X4ae8, 0X4ad0, 0Xa4d0, 0Xd268, 0Xd250, 0Xd528, 0Xb540, 0Xb6a0, 0X96d0, 0X95b0,   //1971-1980
        0X49b0, 0Xa4b8, 0Xa4b0, 0Xb258, 0X6a50, 0X6d40, 0Xada0, 0Xab60, 0X9570, 0X4978,   //1981-1990
        0X4970, 0X64b0, 0X6a50, 0Xea50, 0X6b28, 0X5ac0, 0Xab60, 0X9368, 0X92e0, 0Xc960,   //1991-2000
        0Xd4a8, 0Xd4a0, 0Xda50, 0X5aa8, 0X56a0, 0Xaad8, 0X25d0, 0X92d0, 0Xc958, 0Xa950,   //2001-2010
        0Xb4a0, 0Xb550, 0Xad50, 0X55a8, 0X4ba0, 0Xa5b0, 0X52b8, 0X52b0, 0Xa930, 0X74a8,   //2011-2020
        0X6aa0, 0Xad50, 0X4da8, 0X4b60, 0Xa570, 0Xa4e0, 0Xd260, 0Xe930, 0Xd530, 0X5aa0,   //2021-2030
        0X6b50, 0X96d0, 0X4ae8, 0X4ad0, 0Xa4d0, 0Xd258, 0Xd250, 0Xd520, 0Xdaa0, 0Xb5a0,   //2031-2040
        0X56d0, 0X4ad8, 0X49b0, 0Xa4b8, 0Xa4b0, 0Xaa50, 0Xb528, 0X6d20, 0Xada0, 0X55b0,   //2041-2050
        0X9370, 0X4978, 0X4970, 0X64b0, 0X6a50, 0Xea50, 0X6aa0, 0Xab60, 0Xaae0, 0X92e0,   //2051-2060
        0Xc970, 0Xc960, 0Xd4a8, 0Xd4a0, 0Xda50, 0X5aa8, 0X56a0, 0Xa6d0, 0X52e8, 0X52d0,   //2061-2070
        0Xa958, 0Xa950, 0Xb4a0, 0Xb550, 0Xad50, 0X55a0, 0Xa5d0, 0Xa5b0, 0X52b0, 0Xa938,   //2071-2080
        0X6930, 0X7298, 0X6aa0, 0Xad50, 0X4da8, 0X4b60, 0Xa570, 0X5270, 0Xd160, 0Xe930,   //2081-2090
        0Xd520, 0Xdaa0, 0X6b50, 0X56d0, 0X4ae0, 0Xa4e8, 0Xa2d0, 0Xd150, 0Xd928, 0Xd520    //2091-2100
    };

    // gLunarMonth stores leap months from 1901 to 2050.
    //   one byte stores two years leap month
    final private static byte gLunarMonth[] =
    {
        0X00, 0X50, 0X04, 0X00, 0X20,   //1901-1910
        0X60, 0X05, 0X00, 0X20, 0X70,   //1911-1920
        0X05, 0X00, 0X40, 0X02, 0X06,   //1921-1930
        0X00, 0X50, 0X03, 0X07, 0X00,   //1931-1940
        0X60, 0X04, 0X00, 0X20, 0X70,   //1941-1950
        0X05, 0X00, 0X30, -128, 0X06,   //1951-1960
        0X00, 0X40, 0X03, 0X07, 0X00,   //1961-1970
        0X50, 0X04, 0X08, 0X00, 0X60,   //1971-1980
        0X04, 0X0a, 0X00, 0X60, 0X05,   //1981-1990
        0X00, 0X30, -128, 0X05, 0X00,   //1991-2000
        0X40, 0X02, 0X07, 0X00, 0X50,   //2001-2010
        0X04, 0X09, 0X00, 0X60, 0X04,   //2011-2020
        0X00, 0X20, 0X60, 0X05, 0X00,   //2021-2030
        0X30, -80, 0X06, 0X00, 0X50,   //2031-2040
        0X02, 0X07, 0X00, 0X50, 0X03,   //2041-2050
        0X08, 0X00, 0X60, 0X04, 0X00,   //2051-2060
        0X30, 0X70, 0X05, 0X00, 0X40,   //2061-2070
        -128, 0X06, 0X00, 0X40, 0X03,   //2071-2080
        0X07, 0X00, 0X50, 0X04, 0X08,   //2081-2090
        0X00, 0X60, 0X04, 0X00, 0X20    //2091-2100
    };



    final private static int gLunarFestivalInfo[] = { 0x0100,// "0100"
			0x010F,// "0115"
			0x0505,// "0505"
			0x0707,// "0707"
			0x070F,// "0715"
			0x080F,// "0815"
			0x0909,// "0909"
			0x0C08,// "1208"
			0x0C17,// "1223"
	};
    

    
    final private static int gSolarTermOffsetInfo[] ={    
    0,21208,42467,63836,
    85337,107014,128867,150921,
    173149,195551,218072,240693,
    263343,285989,308563,331033,
    353350,375494,397447,419210,
    440795,462224,483532,504758
    };

    // get lunar festival by lunar date
	public static int getLunarFestivalByLunarDate(int m, int d, int days) {
		int temp;
		int retVal = -1;
		int lm = m;
		int ld = d;

		if((m==12)&&(d>=29)){//for chuxi, the one day before new year.
	        lunarDateInfo  lunarDate = days2lunarDate(days+1);
	        if((lunarDate.lMonth==1)&&(lunarDate.lDay==1)){
	        	lm = 1;
	        	ld = 0;
	        }
		}
		temp=(lm<<8)|(ld&0xFF);
		for(int i=0;i<gLunarFestivalInfo.length;i++)
			if(gLunarFestivalInfo[i]==temp)
				retVal=i;
				//.lunar_festivals[i];
		return retVal;		                       
	}

    //y year n solar term get solar term offset day by date (begin with 0)
	private static int getSolarTermOffsetDayByDate(int y, int n) {
		Calendar cal = Calendar.getInstance();
		cal.set(1900, 0, 6, 2, 5, 0);
		long temp = cal.getTime().getTime();
		if(n<0||n>23)
			temp = 0;
		cal.setTime(new Date((long) ((31556925974.7 * (y - 1900) + gSolarTermOffsetInfo[n] * 60000L) + temp)));
		return cal.get(Calendar.DAY_OF_MONTH);
	}


	public static int getSolarTermByDate(int y, int m, int d) {
//		Log.v("Utils"," getSolarTermByDate year= "+y+" month= "+m+" day= "+d);
		int solarTerm;
		if (m < 0 || m > 11) {
			solarTerm = -1;
			Log.v("Utils"," getSolarTermByDate invalid month");
		}
		m++;//for month 0-11;
		if (d == getSolarTermOffsetDayByDate(y, (m - 1) * 2)) {
			Log.v("Utils"," getSolarTermByDate get month = "+m+" solar_term_num= "+(m-1)*2+" day= "+d+ " got_terms= "+(m - 1) * 2);
			solarTerm = (m - 1) * 2;
		} else if (d == getSolarTermOffsetDayByDate(y, (m - 1) * 2 + 1)) {
			Log.v("Utils"," getSolarTermByDate get month = "+m+" solar_term_num= "+(m-1)*2+1+" day= "+d+ " got_terms= "+(m - 1) * 2+1);
			solarTerm = (m - 1) * 2 + 1;
		} else { // not solar term
//			Log.v("Utils"," getSolarTermByDate not match solar term ");
			solarTerm = -1;
		}
		return solarTerm;
	}
   
    /**
     * Gets the GanZhi and ShengXiao.
     * @param year
     * @return
     */
    final private static String lunarYearName(Context context, int year) {
        int num = year - 4;
        int temp = num % 12;
        Log.v(TAG,"year = "+year+" num = "+num+" temp = "+temp);
        
    	String [] tianganArray= context.getResources().getStringArray(R.array.tiangan);
        Log.v(TAG,"tianganArray = " + tianganArray.toString());      
    	String [] dizhiArray= context.getResources().getStringArray(R.array.dizhi);        
    	String [] shengxiaoArray= context.getResources().getStringArray(R.array.shengxiao);        
        return (tianganArray[num % 10] + dizhiArray[temp] + "(" + shengxiaoArray[temp] + ")"+context.getResources().getString(R.string.year));
    }

    /**
     * Gets the month name in lunar.
     * @param month
     * @return
     */
    final private static String lunarMonthName(Context context, int month, boolean leap) {
        if (month<1 || month>12)
        {
            android.util.Log.e("CalUtils", "  illegal month: " + month);
            return "";
        }

        int num = month - 1;

        // Leap month has no special expression per CxD design.
        //        return leap ?
        //               (context.getString(R.string.leap) + context.getString(lunarMonths[num])) :
        //               context.getString(lunarMonths[num]);
    	String [] lunarMonthArray= context.getResources().getStringArray(R.array.lunar_month);         
        return lunarMonthArray[num];
    }

    /**
     * Gets the monthday name in lunar.
     * @param monthday
     * @return
     */
    final private static String lunarMonthdayName(Context context, int monthday) {
        if(monthday<1 || monthday>30)
        {
            android.util.Log.e("CalUtils", "  illegal monthday: " + monthday);
            return "";
        }

        int index1;
        index1 = monthday / 10;
        if(monthday == 10)
            index1 = 4;
        else if(monthday == 20)
            index1 = 5;

    	String [] lunarDayPart1Array= context.getResources().getStringArray(R.array.lunar_day_part1);         
    	String [] lunarDayPart2Array= context.getResources().getStringArray(R.array.lunar_day_part2);         
        return (lunarDayPart1Array[index1] + lunarDayPart2Array[monthday%10]);
    }


	/**
	 * 
	 */
	private static lunarDateInfo days2lunarDate(int days) {
		int spanDays;
		lunarDateInfo lunarDate = new lunarDateInfo();
		
		spanDays = days;
		if(spanDays < 49)
		{
			lunarDate.lYear = LUNAR_START_YEAR - 1;
		    if(spanDays < 19)
		    {
		    	lunarDate.lMonth = 11;
		    	lunarDate.lDay = 11 + spanDays;
		    }
		    else
		    {
		    	lunarDate.lMonth = 12;
		    	lunarDate.lDay = spanDays - 18;
		    }

		    android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);
		    lunarDate.lessSpanFlag = true;
		    return lunarDate;
		}

		spanDays -= 49;
		lunarDate.lYear = LUNAR_START_YEAR;
		lunarDate.lMonth = 1;
		lunarDate.lDay = 1;

		// Just speed up the calculation if the date is later than Lunar Calendar Date 2009/1/1.
		//   The distance from (lunar) 1901/1/1 to 2009/1/1 is 39423 days.
		//   It is more common in using, and could be removed freely. 
		if(spanDays > 39423)
		{
			lunarDate.lYear = 2009;
		    spanDays -= 39423;
		}

		// Year
		int tmp = daysInLunarYear(lunarDate.lYear);
		while(spanDays >= tmp)
		{
		    spanDays -= tmp;
		    tmp = daysInLunarYear(++lunarDate.lYear);
		}

		// Month
		tmp = daysInLunarMonth(lunarDate.lYear, lunarDate.lMonth)  & 0x00ffff;
		int leapMonthInThisYear = leapMonthInYear(lunarDate.lYear);
		while(spanDays >= tmp)
		{
		    spanDays -= tmp;
		    if(lunarDate.lMonth == leapMonthInThisYear)
		    {
		        //tmp  = HIWORD(daysInLunarMonth(lYear, lMonth));
		        tmp = daysInLunarMonth(lunarDate.lYear, lunarDate.lMonth) >> 16;
		        if(spanDays < tmp)
		        {
		        	lunarDate.leap = true;
		            break;
		        }
		        spanDays -= tmp;
		    }
		    tmp = daysInLunarMonth(lunarDate.lYear, ++lunarDate.lMonth) & 0x00ffff;
		}

		// Day
		lunarDate.lDay += spanDays;
		return lunarDate;
	}
	
	//fwr687 begin
	public static String lunarYear(Context context, Time time) {
        String lunar = "";
        if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
        {
            android.util.Log.e("CalUtils", "  illegal year: " + time.year);
            return lunar;
        }

        // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
        //   the days difference between Gregorian Calendar Date 1/1 and 2/19
        //   is 49 days.

        Calendar calen = Calendar.getInstance();
        calen.set(time.year, time.month, time.monthDay);
        long spanMills;
        spanMills = calen.getTimeInMillis();
        calen.set(1901, 0, 1);
        spanMills -= calen.getTimeInMillis();
        int days = (int) (spanMills / (86400000L));  // 1000 * 60 * 60 * 24

        lunarDateInfo  lunarDate = days2lunarDate(days);
        android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear );
        //fwr687 
        String lunarLabel = context.getResources().getString(R.string.lunar_lable);
        
		lunar = lunarLabel + lunarYearName(context, lunarDate.lYear);
		
        return lunar;
    }
	//fwr687 end
    /**
     * Provides functions to calculate Chinese lunar calendar month based on
     *   western calendar.
     * FIXME: Should it be in native code?
     */
    public static String lunarMonth(Context context, Time time) {
        String lunar = "";
        if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
        {
            android.util.Log.e("CalUtils", "  illegal year: " + time.year);
            return lunar;
        }

        // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
        //   the days difference between Gregorian Calendar Date 1/1 and 2/19
        //   is 49 days.

        Calendar calen = Calendar.getInstance();
        calen.set(time.year, time.month, time.monthDay);
        long spanMills;
        spanMills = calen.getTimeInMillis();
        calen.set(1901, 0, 1);
        spanMills -= calen.getTimeInMillis();
        int days = (int) (spanMills / (86400000L));  // 1000 * 60 * 60 * 24

        // FIXME: Any DST issue?
        // if( calen.add(days) != time )
        //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
        // TODO: And shall we use the yeardays to calculate the span days?
        // if (year1 == year2) days = yearday2 - yearday1;
        // else {sum();}

        // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

        lunarDateInfo  lunarDate = days2lunarDate(days);
        android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);
        //fwr687 
        String lunarLabel = context.getResources().getString(R.string.lunar_lable);
        if (lunarDate.lessSpanFlag == true)
			lunar = lunarLabel + lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, false);
		else
			lunar = lunarLabel + lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, lunarDate.leap);
        return lunar;
    }
    
    /**
     * Provides functions to calculate Chinese lunar calendar date based on
     *   western calendar.
     * FIXME: Should it be in native code?
     */
    public static String lunarDate(Context context, Time time) {
        String lunar = "";
        //fwr687 
        String lunarLabel = context.getResources().getString(R.string.lunar_lable);
        if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
        {
            android.util.Log.e("CalUtils", "  illegal year: " + time.year);
            return lunar;
        }

        // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
        //   the days difference between Gregorian Calendar Date 1/1 and 2/19
        //   is 49 days.

        Calendar calen = Calendar.getInstance();
        calen.set(time.year, time.month, time.monthDay);
        long spanMills;
        spanMills = calen.getTimeInMillis();
        calen.set(1901, 0, 1);
        spanMills -= calen.getTimeInMillis();
        int days = (int) (spanMills / (86400000L));  // 1000 * 60 * 60 * 24

        // FIXME: Any DST issue?
        // if( calen.add(days) != time )
        //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
        // TODO: And shall we use the yeardays to calculate the span days?
        // if (year1 == year2) days = yearday2 - yearday1;
        // else {sum();}

        // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

        lunarDateInfo  lunarDate = days2lunarDate(days);        

        android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);
        if (lunarDate.lessSpanFlag == true)
        	 //fwr687 
        	lunar = lunarLabel+lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, false)
					+ lunarMonthdayName(context, lunarDate.lDay);
		else
			//fwr687 
			lunar = lunarLabel+lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, lunarDate.leap)
					+ lunarMonthdayName(context, lunarDate.lDay);
       
        //fwr687 begin        
        int lunarFestivalIndex = getLunarFestivalByLunarDate(lunarDate.lMonth,lunarDate.lDay,days);
        if(lunarFestivalIndex!=-1){
        	String [] lunarFestivalArray= context.getResources().getStringArray(R.array.lunar_festival);
        	 //fwr687 
        	lunar += " "+lunarFestivalArray[lunarFestivalIndex]; 
        	return lunar;
        }
        
        int solarTermIndex=getSolarTermByDate(time.year, time.month, time.monthDay);
        if(solarTermIndex!=-1){
        	String [] solarTermArray= context.getResources().getStringArray(R.array.solar_term);
        	 //fwr687 
        	lunar += " "+solarTermArray[solarTermIndex];
        	return lunar;
        }
        //fwr687 end
        return lunar;
    }


	public static lunarInfo monthlyViewLunarDate(Context context, Time time) {
    	if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
    	{
    		android.util.Log.e("CalUtils", "  illegal year: " + time.year);
    		return null;
    	}
    	
        lunarInfo lunarData = new lunarInfo();
        
        // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
        //   the days difference between Gregorian Calendar Date 1/1 and 2/19
        //   is 49 days.

        Calendar calen = Calendar.getInstance();
        calen.set(time.year, time.month, time.monthDay);
        long spanMills;
        spanMills = calen.getTimeInMillis();
        calen.set(1901, 0, 1);
        spanMills -= calen.getTimeInMillis();
        int days = (int) (spanMills / (86400000L));  // 1000 * 60 * 60 * 24

        // FIXME: Any DST issue?
        // if( calen.add(days) != time )
        //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
        // TODO: And shall we use the yeardays to calculate the span days?
        // if (year1 == year2) days = yearday2 - yearday1;
        // else {sum();}

        // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

        lunarDateInfo  lunarDate = days2lunarDate(days);
        
    	int lunarFestivalIndex = getLunarFestivalByLunarDate(lunarDate.lMonth,lunarDate.lDay,days);
    	if(lunarFestivalIndex!=-1){
        	String [] lunarFestivalArray= context.getResources().getStringArray(R.array.lunar_festival);
    		lunarData.lunarStr =  lunarFestivalArray[lunarFestivalIndex];
    		lunarData.lunarType = lunarInfo.LUNAR_TYPE_FESTIVAL;
    		return lunarData;
    	}
        int solarTermIndex=getSolarTermByDate(time.year, time.month, time.monthDay);
        if(solarTermIndex!=-1){
        	String [] solarTermArray= context.getResources().getStringArray(R.array.solar_term);
    		lunarData.lunarStr = solarTermArray[solarTermIndex];
    		lunarData.lunarType = lunarInfo.LUNAR_TYPE_SOLAR_TEAM;
    		return lunarData;
    	}
    	
    	if (lunarDate.lDay == 1) {
			if (lunarDate.lessSpanFlag == true)
				lunarData.lunarStr = lunarMonthName(context, lunarDate.lMonth, false);
			else
				lunarData.lunarStr = lunarMonthName(context, lunarDate.lMonth, lunarDate.leap);
		} else
			lunarData.lunarStr = lunarMonthdayName(context, lunarDate.lDay);
		lunarData.lunarType = lunarInfo.LUNAR_TYPE_NORMAL;
    	return lunarData;
    }

    /**
     * Calculates the days in a lunar calendar year.
     */
    private static int daysInLunarYear(int year) {
        int days = 0;
        for( int i=1; i<=12; i++ )
        {
            int tmp = daysInLunarMonth(year, i);
            days += (tmp >> 16);
            days += (tmp) & 0xFF;
        }
        return days;
    }

    /**
     * Gets days of LunarMonth in LunarYear. If LunarMonth is a leap month,
     *   higher 16 bit is the days of next LunarMonth; Otherwise, higher 16 bit is 0.
     */
    private static int daysInLunarMonth(int year, int month) {
        if (( year < LUNAR_START_YEAR ) || ( year > LUNAR_END_YEAR ))
            return 30;

        int high = 0;
        int low = 29;
        int iBit = 16 - month;

        int leapMonths = leapMonthInYear(year);
        if((leapMonths > 0) && (month > leapMonths))
            iBit --;

        int spanYears = year - LUNAR_START_YEAR;
        if((gLunarMonthDay[spanYears] & (1<<iBit)) != 0)
            low ++;

        if(month == leapMonths)
        {
            if((gLunarMonthDay[spanYears] & (1<<(iBit-1))) != 0)
                 high = 30;
            else
                 high = 29;
        }

        int result = high;
        result = result << 16;
        result |= low;
        return result;
    }

    /**
     * Gets the leap month in a lunar year.
     */
    private static int leapMonthInYear(int year) {
        int index = (year - LUNAR_START_YEAR) / 2;
        if (index >= gLunarMonth.length)
        {
            android.util.Log.e("CalUtils", "  Fault - leap month index illegal!");
            return 0;
        }
        byte flag = gLunarMonth[index];
        if((year - LUNAR_START_YEAR)%2 != 0)
             return (int) flag & 0xFF;
        else
             return (int) (flag>>4) & 0x0F;
    }

}
