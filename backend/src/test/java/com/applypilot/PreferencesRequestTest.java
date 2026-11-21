package com.applypilot;

import com.applypilot.user.dto.PreferencesRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PreferencesRequestTest {
    @Test void rejects_inverted_experience_range() {
        var request = new PreferencesRequest(List.of("Java Developer"), List.of("Remote"), List.of("Remote"), 3, 2, true);
        assertThat(request.isExperienceRangeValid()).isFalse();
    }
}
