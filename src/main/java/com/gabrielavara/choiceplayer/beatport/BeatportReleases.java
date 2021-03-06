package com.gabrielavara.choiceplayer.beatport;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class BeatportReleases implements BeatportSearchOutput {
    private List<BeatportRelease> releases;
}
