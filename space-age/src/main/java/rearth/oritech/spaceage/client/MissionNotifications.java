package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Small global notifications for mission events which may happen outside Mission Control. */
public final class MissionNotifications {
    private static final SystemToast.SystemToastId SURVEY_RESULTS = new SystemToast.SystemToastId();

    public static void showSurveyResults(int discovered, int updated) {
        SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), SURVEY_RESULTS,
                Component.translatable("notification.oritech_space_age.survey_results"),
                Component.translatable("notification.oritech_space_age.survey_results_detail", discovered, updated));
    }

    private MissionNotifications() { }
}
