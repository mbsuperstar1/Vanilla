package org.getspout.vanilla.entity.living.hostile;

import org.getspout.api.metadata.MetadataStringValue;
import org.getspout.vanilla.entity.living.MonsterEC;
import org.getspout.vanilla.mobs.MobID;

public class SilverfishEC extends MonsterEC {
	@Override
	public void onAttached() {
		super.onAttached();
		parent.setMetadata(MobID.KEY, new MetadataStringValue(MobID.Silverfish.id));
	}

	@Override
	public void onTick(float dt) {
		super.onTick(dt);
	}

}
