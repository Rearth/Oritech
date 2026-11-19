package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import rearth.oritech.spaceage.network.MissionNetworking.SurveyNotice.Kind;

/**
 * Small global notifications for mission events which may happen outside Mission Control.
 */
public final class MissionNotifications {

    private static final SystemToast.SystemToastId SURVEY_RESULTS = new SystemToast.SystemToastId();
    private static final SystemToast.SystemToastId EARTH_SURVEY = new SystemToast.SystemToastId();
    private static final SystemToast.SystemToastId MAP_EXPORT = new SystemToast.SystemToastId();

    private MissionNotifications() {
    }

    public static void showSurveyResults(int discovered, int updated) {

        SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), SURVEY_RESULTS,
                Component.translatable("notification.oritech_space_age.survey_results"),
                Component.translatable("notification.oritech_space_age.survey_results_detail", discovered, updated));
    }

    public static void showSurveyNotice(Kind kind) {

        var detail = switch (kind) {
            case EARTH_RECEIVED -> "notification.oritech_space_age.earth_survey_received";
            case MAP_EXPORTED -> "notification.oritech_space_age.earth_map_exported";
            case INVENTORY_FULL -> "notification.oritech_space_age.earth_map_inventory_full";
        };
        SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), kind == Kind.EARTH_RECEIVED ? EARTH_SURVEY : MAP_EXPORT,
                Component.translatable("screen.oritech_space_age.earth_survey.reports"), Component.translatable(detail));
    }
}
