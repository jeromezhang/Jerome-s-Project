/*fwr687*/
package com.android.calendar;


import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TextView;

public class GoToDateActivity extends Activity{
	
	 private Time mCurrentDate;
	 private TextView today_solar_date;
	 private TextView today_lunar_date;
	 private TextView checked_solar_date;
	 private TextView checked_lunar_date;
	 private int flags ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.go_to_date);
		DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        today_solar_date = (TextView)findViewById(R.id.today_solar_date);
        today_lunar_date = (TextView)findViewById(R.id.today_lunar_date);
        checked_solar_date = (TextView)findViewById(R.id.checked_solar_date);
        checked_lunar_date = (TextView)findViewById(R.id.checked_lunar_date);
        
        mCurrentDate = new Time();
        long now = System.currentTimeMillis();
        mCurrentDate.set(now);
        flags = DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_SHOW_WEEKDAY
        | DateUtils.FORMAT_SHOW_DATE;
        String dateRange ="  " + DateUtils.formatDateRange(GoToDateActivity.this, now, now, flags);
        
        today_solar_date.setText(dateRange);
        today_lunar_date.setText(Utils.lunarDate(this.getBaseContext(), mCurrentDate));
       
        datePicker.init(mCurrentDate.year, mCurrentDate.month, mCurrentDate.monthDay, new OnDateChangedListener(){

			
			public void onDateChanged(DatePicker view, int year,
					int monthOfYear, int dayOfMonth) {
				// TODO Auto-generated method stub
				Time changedDate = new Time();
				changedDate.set(dayOfMonth, monthOfYear, year);
				long millis = changedDate.toMillis(false);
				 String dateRange ="  " +  DateUtils.formatDateRange(GoToDateActivity.this, millis, millis, flags);
				checked_solar_date.setText(dateRange);
				checked_lunar_date.setText(Utils.lunarDate(GoToDateActivity.this, changedDate));
			}		    	
        });
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
}
