package com.getvisitapp.google_fit.pojo;

import androidx.annotation.Keep;

/**
 * Created by shashvat on 06/05/18.
 */

@Keep
public class ActivitySummaryGoal {
    private int stepsGoal;
    private int distanceGoal;
    private int stepsProgress;
    private int distanceProgress;
    private int caloriesBurntProgress;
    private int caloriesIntakeProgress;
    private int sleepProgress;

    public void setNutritionProgress(double nutritionProgress) {
        this.nutritionProgress = nutritionProgress;
    }

    private double nutritionProgress;
    private int sleepGoal;

    private int totalSteps;
    private int totalDistanceInMeters;
    private int totalSleepInSeconds;

    public HealthDataGraphValues steps;
    public HealthDataGraphValues calorie;
    public HealthDataGraphValues distance;
    public HealthDataGraphValues sleep;

    public ActivitySummaryGoal() {

    }

    public ActivitySummaryGoal(int stepsGoal, int distanceGoal, double nutritionProgress, int sleepGoal) {
        this.stepsGoal = stepsGoal;
        this.distanceGoal = distanceGoal;
        this.nutritionProgress = nutritionProgress;
        this.sleepGoal = sleepGoal;
    }


    public ActivitySummaryGoal(HealthDataGraphValues steps, HealthDataGraphValues distance, HealthDataGraphValues calorie, HealthDataGraphValues sleep) {
        this.steps = steps;
        this.calorie = calorie;
        this.distance = distance;
        this.sleep = sleep;
    }


    public ActivitySummaryGoal(double stepsProgress, double distanceProgress, double caloriesBurntProgress, double caloriesIntakeProgress, double sleepProgress) {
        this.stepsProgress = (int) (stepsProgress * 100);
        this.distanceProgress = (int) (distanceProgress * 100);
        this.caloriesBurntProgress = (int) (caloriesBurntProgress * 100);
        this.caloriesIntakeProgress = (int) (caloriesIntakeProgress * 100);
        this.sleepProgress = (int) (sleepProgress * 100);
    }


    public int getStepsGoal() {
        return stepsGoal;
    }

    public int getDistanceGoal() {
        return distanceGoal;
    }


    public int getSleepGoal() {
        return sleepGoal;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getTotalDistanceInMeters() {
        return totalDistanceInMeters;
    }

    public void setTotalDistanceInMeters(int totalDistanceInMeters) {
        this.totalDistanceInMeters = totalDistanceInMeters;
    }

    public int getTotalSleepInSeconds() {
        return totalSleepInSeconds;
    }

    public void setTotalSleepInSeconds(int totalSleepInSeconds) {
        this.totalSleepInSeconds = totalSleepInSeconds;
    }


    public int getStepsProgress() {
        return (int) ((((double) totalSteps / stepsGoal)) * 100);
    }

    public int getSleepProgress() {
        return (int) (((double) totalSleepInSeconds / (sleepGoal * 60)) * 100);
    }

    public int getDistanceProgress() {
        return (int) (((double) totalDistanceInMeters / distanceGoal) * 100);
    }

    public int getNutritionProgress() {
        return (int) (nutritionProgress * 100);
    }

    public int getCaloriesIntakeProgress() {
        return (caloriesIntakeProgress);
    }


    public int getCaloriesBurntProgress() {
        return caloriesBurntProgress;
    }

    public int getStepsProgressForFitbit() {
        return stepsProgress;
    }

    public int getDistanceProgressForFitbit() {
        return distanceProgress;
    }


    public int getSleepProgressForFitbit() {
        return sleepProgress;
    }

    @Override
    public String toString() {
        try {
            return "stepsGoal [" + stepsGoal + "], distanceGoal[" + distanceGoal + "], nutritionProgress [" + nutritionProgress + "], sleepgoal[" + sleepGoal + "]";
        } catch (Exception e) {
            return super.toString();
        }
    }
}
