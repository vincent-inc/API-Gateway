package com.vincentcrop.vshop.APIGateway.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Time {
    public static final int MAX_MONTH = 12;
    public static final int MAX_HOURS = 24;
    public static final int MAX_MINUTE = 60;
    public static final int MAX_SECOND = 60;

    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("America/New_York");

    private int year;
    private int month;
    private int day;
    private int hours;
    private int minute;
    private int second;

    @Builder.Default
    private boolean bypassMax = false;

    public Time() {
        LocalDate localDate = LocalDate.now(DEFAULT_ZONE_ID);
        LocalTime localTime = LocalTime.now(DEFAULT_ZONE_ID);

        this.month = localDate.getMonthValue();
        this.day = localDate.getDayOfMonth();
        this.year = localDate.getYear();
        this.hours = localTime.getHour();
        this.minute = localTime.getMinute();
        this.second = localTime.getSecond();
    }

    public Time(ZoneId zoneId) {
        LocalDate localDate = LocalDate.now(zoneId);
        LocalTime localTime = LocalTime.now(zoneId);

        this.month = localDate.getMonthValue();
        this.day = localDate.getDayOfMonth();
        this.year = localDate.getYear();
        this.hours = localTime.getHour();
        this.minute = localTime.getMinute();
        this.second = localTime.getSecond();
    }

    public Time(int year, int month, int day, int hours, int minute, int second) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hours = hours;
        this.minute = minute;
        this.second = second;
    }

    public static Time now() {
        return new Time();
    }

    public static Time now(ZoneId zoneId) {
        return new Time(zoneId);
    }

    public LocalDate toLocalDate() {
        return LocalDate.of(this.year, this.month, this.day);
    }

    public LocalTime toLocalTime() {
        return LocalTime.of(this.hours, this.minute, this.second);
    }

    public boolean isBefore(Time time) {
        if (this.toLocalDate().isBefore(time.toLocalDate()))
            return true;

        if (this.toLocalTime().isBefore(time.toLocalTime()))
            return true;

        return false;
    }

    public boolean isAfter(Time time) {
        if (this.toLocalDate().isAfter(time.toLocalDate()))
            return true;

        if (this.toLocalTime().isAfter(time.toLocalTime()))
            return true;

        return false;
    }

    public int getMaxDay(int month) {
        switch (month) {
            case 2:
                return 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    public int getMaxDay() {
        return this.getMaxDay(this.month);
    }

    public Time increaseYear(int year) {
        this.year += year;
        return this;
    }

    public Time increaseMonth(int month) {
        this.month += month;
        if (!this.bypassMax && this.month > MAX_MONTH) {
            this.increaseYear(this.month / MAX_MONTH);
            this.month = this.month % MAX_MONTH;
        }
        return this;
    }

    public Time increaseDay(int day) {
        this.day += day;
        int MAX_DAY = this.getMaxDay();
        if (!this.bypassMax && this.day > MAX_DAY) {
            this.increaseMonth(this.day / MAX_DAY);
            this.day = this.day % this.getMaxDay(this.month);
        }
        return this;
    }

    public Time increaseHours(int hours) {
        this.hours += hours;
        if (!this.bypassMax && this.hours >= MAX_HOURS) {
            this.increaseDay(this.hours / MAX_HOURS);
            this.hours = this.hours % MAX_HOURS;
        }
        return this;
    }

    public Time increaseMinute(int minute) {
        this.minute += minute;
        if (!this.bypassMax && this.minute >= MAX_MINUTE) {
            this.increaseHours(this.minute / MAX_MINUTE);
            this.minute = this.minute % MAX_MINUTE;
        }
        return this;
    }

    public Time increaseSecond(int second) {
        this.second += second;
        if (!this.bypassMax && this.second >= MAX_SECOND) {
            this.increaseMinute(this.second / MAX_SECOND);
            this.second = this.second % MAX_SECOND;
        }
        return this;
    }

    public Time increaseTime(Time time) {
        this.increaseYear(time.getYear());
        this.increaseMonth(time.getMonth());
        this.increaseDay(this.getDay());
        this.increaseHours(this.getHours());
        this.increaseMinute(this.getMinute());
        this.increaseSecond(this.getSecond());
        return this;
    }

    public String toSpring() {
        return String.format("%s | %s", this.getDate(), this.getTime());
    }

    public String getDate() {
        return String.format("%s/%s/%s", this.month, this.day, this.year);
    }

    public String getTime() {
        return String.format("%s:%s:%s", this.hours, this.minute, this.second);
    }

    public double sumMinutes() {
        double yearToMonth = (this.year * 12) + this.month;
        double monthToDay = (yearToMonth * 30) + this.day;
        double dayToHours = (monthToDay * 24) + this.hours;
        double hoursToMinutes = (dayToHours * 60) + this.minute;
        double secondToMinutes = (double) this.second / 60;
        return hoursToMinutes + secondToMinutes;
    }

    public long sumSeconds() {
        double sumMinutes = this.sumMinutes();
        return (long) (sumMinutes * 60);
    }

    public Duration toDuration() {
        var zero = new Time(0, 0, 0, 0, 0, 0);

        var timeTarget = this.toLocalTime();
        var timeDuration = Duration.between(zero.toLocalTime(), timeTarget);

        var dayTarget = this.toLocalDate();
        var dayDuration = Duration.between(zero.toLocalDate(), dayTarget);

        var duration = timeDuration.plus(dayDuration);

        return duration;
    }
}
