package com.retail.dispatch.service;

import java.util.List;

public record BlocksResult(boolean covered, List<BlockOption> blocks) {

	static BlocksResult notCovered() {
		return new BlocksResult(false, List.of());
	}

}
