/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.navigation.model;

import static androidx.car.app.TestUtils.createDateTimeWithZone;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.TimeUnit;

/** Tests for {@link NavigationTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class NavigationTemplateTest {
    private final ActionStrip mActionStrip =
            new ActionStrip.Builder().addAction(TestUtils.createAction("test", null)).build();
    private final Maneuver mManeuver =
            new Maneuver.Builder(Maneuver.TYPE_FERRY_BOAT).setIcon(CarIcon.APP_ICON).build();
    private final Step mCurrentStep =
            new Step.Builder("Go Straight").setManeuver(mManeuver).setRoad("405").build();
    private final Distance mCurrentDistance =
            Distance.create(/* displayDistance= */ 100, Distance.UNIT_METERS);

    @Test
    public void noActionStrip_throws() {
        assertThrows(IllegalStateException.class, () -> NavigationTemplate.builder().build());
    }

    /** Tests basic construction of a template with a minimal data. */
    @Test
    public void createMinimalInstance() {
        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setNavigationInfo(
                                new RoutingInfo.Builder().setCurrentStep(mCurrentStep,
                                        mCurrentDistance).build())
                        .setActionStrip(mActionStrip)
                        .build();
        RoutingInfo routingInfo = (RoutingInfo) template.getNavigationInfo();
        assertThat(routingInfo.getCurrentStep()).isEqualTo(mCurrentStep);
        assertThat(routingInfo.getNextStep()).isNull();
        assertThat(template.getBackgroundColor()).isNull();
        assertThat(template.getDestinationTravelEstimate()).isNull();
        assertThat(template.getActionStrip()).isEqualTo(mActionStrip);
    }

    /** Tests construction of a template with all data. */
    @Test
    public void createFullInstance() {
        Maneuver nextManeuver =
                new Maneuver.Builder(Maneuver.TYPE_U_TURN_LEFT).setIcon(CarIcon.APP_ICON).build();
        Step nextStep = new Step.Builder("Turn Around").setManeuver(nextManeuver).setRoad(
                "520").build();

        TravelEstimate travelEstimate =
                TravelEstimate.create(
                        Distance.create(/* displayDistance= */ 20, Distance.UNIT_METERS),
                        TimeUnit.HOURS.toSeconds(1),
                        createDateTimeWithZone("2020-05-14T19:57:00-07:00", "US/Pacific"));
        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setNavigationInfo(
                                new RoutingInfo.Builder()
                                        .setCurrentStep(mCurrentStep, mCurrentDistance)
                                        .setNextStep(nextStep)
                                        .build())
                        .setBackgroundColor(CarColor.BLUE)
                        .setDestinationTravelEstimate(travelEstimate)
                        .setActionStrip(mActionStrip)
                        .build();
        RoutingInfo routingInfo = (RoutingInfo) template.getNavigationInfo();
        assertThat(routingInfo.getCurrentStep()).isEqualTo(mCurrentStep);
        assertThat(routingInfo.getCurrentDistance()).isEqualTo(mCurrentDistance);
        assertThat(routingInfo.getNextStep()).isEqualTo(nextStep);
        assertThat(template.getBackgroundColor()).isEqualTo(CarColor.BLUE);
        assertThat(template.getDestinationTravelEstimate()).isEqualTo(travelEstimate);
        assertThat(template.getActionStrip()).isEqualTo(mActionStrip);
    }

    @Test
    public void equals() {
        TravelEstimate travelEstimate =
                TravelEstimate.create(
                        Distance.create(/* displayDistance= */ 20, Distance.UNIT_METERS),
                        TimeUnit.HOURS.toSeconds(1),
                        createDateTimeWithZone("2020-05-14T19:57:00-07:00", "US/Pacific"));

        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(
                                new Lane.Builder()
                                        .addDirection(LaneDirection.create(
                                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                                        .build())
                        .setLanesImage(CarIcon.ALERT)
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setDestinationTravelEstimate(travelEstimate)
                        .setNavigationInfo(
                                new RoutingInfo.Builder()
                                        .setCurrentStep(currentStep, currentDistance)
                                        .setJunctionImage(CarIcon.ALERT)
                                        .setNextStep(currentStep)
                                        .build())
                        .setBackgroundColor(CarColor.BLUE)
                        .build();

        assertThat(template)
                .isEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(mActionStrip)
                                .setDestinationTravelEstimate(travelEstimate)
                                .setNavigationInfo(
                                        new RoutingInfo.Builder()
                                                .setCurrentStep(currentStep, currentDistance)
                                                .setJunctionImage(CarIcon.ALERT)
                                                .setNextStep(currentStep)
                                                .build())
                                .setBackgroundColor(CarColor.BLUE)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        NavigationTemplate template = NavigationTemplate.builder().setActionStrip(
                mActionStrip).build();

        assertThat(template)
                .isNotEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                TestUtils.createAction("title2", null)).build())
                                .build());
    }

    @Test
    public void notEquals_differentTravelEstimate() {
        TravelEstimate travelEstimate =
                TravelEstimate.create(
                        Distance.create(/* displayDistance= */ 20, Distance.UNIT_METERS),
                        TimeUnit.HOURS.toSeconds(1),
                        createDateTimeWithZone("2020-05-14T19:57:00-07:00", "US/Pacific"));

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setDestinationTravelEstimate(travelEstimate)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(mActionStrip)
                                .setDestinationTravelEstimate(
                                        TravelEstimate.create(
                                                Distance.create(/* displayDistance= */ 21000,
                                                        Distance.UNIT_METERS),
                                                TimeUnit.HOURS.toSeconds(1),
                                                createDateTimeWithZone("2020-05-14T19:57:00-07:00",
                                                        "US/Pacific")))

                                .build());
    }

    @Test
    public void notEquals_differentCurrentStep() {
        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(
                                new Lane.Builder()
                                        .addDirection(LaneDirection.create(
                                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                                        .build())
                        .setLanesImage(CarIcon.APP_ICON)
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(
                                new RoutingInfo.Builder().setCurrentStep(currentStep,
                                        currentDistance).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(new RoutingInfo.Builder()
                                .setCurrentStep(new Step.Builder("do a back flip")
                                                .addLane(new Lane.Builder()
                                                        .addDirection(LaneDirection.create(
                                                                LaneDirection.SHAPE_NORMAL_LEFT,
                                                                false))
                                                        .build())
                                                .setLanesImage(CarIcon.APP_ICON)
                                                .build(),
                                        currentDistance)
                                .build())
                        .build());
    }

    @Test
    public void notEquals_differentCurrentDistance() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.APP_ICON)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(
                                new RoutingInfo.Builder().setCurrentStep(currentStep,
                                        currentDistance).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(mActionStrip)
                                .setNavigationInfo(
                                        new RoutingInfo.Builder()
                                                .setCurrentStep(
                                                        currentStep,
                                                        Distance.create(/* displayDistance= */ 200,
                                                                Distance.UNIT_METERS))
                                                .build())
                                .build());
    }

    @Test
    public void notEquals_differentJunctionImage() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.ALERT)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(
                                new RoutingInfo.Builder()
                                        .setCurrentStep(currentStep, currentDistance)
                                        .setJunctionImage(CarIcon.ALERT)
                                        .setNextStep(currentStep)
                                        .build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(mActionStrip)
                                .setNavigationInfo(
                                        new RoutingInfo.Builder()
                                                .setCurrentStep(currentStep, currentDistance)
                                                .setJunctionImage(CarIcon.ERROR)
                                                .setNextStep(currentStep)
                                                .build())
                                .build());
    }

    @Test
    public void notEquals_differentNextStep() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.ALERT)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(
                                new RoutingInfo.Builder()
                                        .setCurrentStep(currentStep, currentDistance)
                                        .setNextStep(currentStep)
                                        .build())
                        .build();

        assertThat(template)
                .isNotEqualTo(NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setNavigationInfo(new RoutingInfo.Builder()
                                .setCurrentStep(currentStep, currentDistance)
                                .setNextStep(new Step.Builder("Do a backflip")
                                        .addLane(new Lane.Builder()
                                                .addDirection(LaneDirection.create(
                                                        LaneDirection.SHAPE_NORMAL_LEFT,
                                                        false))
                                                .build())
                                        .setLanesImage(CarIcon.ALERT)
                                        .build())
                                .build())
                        .build());
    }

    @Test
    public void notEquals_differentBackgroundColors() {
        NavigationTemplate template =
                NavigationTemplate.builder()
                        .setActionStrip(mActionStrip)
                        .setBackgroundColor(CarColor.BLUE)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        NavigationTemplate.builder()
                                .setActionStrip(mActionStrip)
                                .setBackgroundColor(CarColor.GREEN)
                                .build());
    }
}
