/*
 * This file is part of World Downloader: A mod to make backups of your
 * multiplayer worlds.
 * http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2520465
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see http://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.chunk.ChunkManager;

/**
 * Tests the data contained within StandardEntityManagers.
 */
@RunWith(Parameterized.class)
public class StandardEntityManagersTest {
	static {
		MaybeMixinTest.init();
	}

	@Parameters(name="{0}")
	public static List<Object[]> data() {
		return StandardEntityManagers.VANILLA.getProvidedEntities().stream()
				.sorted()
				.map(id->new Object[]{id})
				.collect(Collectors.toList());
	}

	/**
	 * Entity identifier to test.
	 */
	private final String identifier;
	/**
	 * EntityType associated with the entity to test
	 */
	private final EntityType<?> type;

	public StandardEntityManagersTest(String identifier) {
		this.identifier = identifier;
		this.type = Registry.ENTITY_TYPE.func_218349_b(new ResourceLocation(identifier)).get();
	}

	/**
	 * Checks that the identifier matches the class.
	 */
	@Test
	public void testIdentifier() throws Exception {
		TestWorld.ServerWorld world = TestWorld.makeServer();
		Entity entity = this.type.create(world);
		assertThat(StandardEntityManagers.VANILLA.getIdentifierFor(entity), is(identifier));
		world.close();
	}

	/**
	 * Checks that the range associated with the entity is the actual range assigned by EntityTracker.
	 */
	@Test
	public void testVanillaRange() throws Exception {
		TestWorld.ServerWorld world = TestWorld.makeServer();

		class DerivedTracker extends ChunkManager {
			public DerivedTracker() {
				super(null, null, null, null, null, null, null, null, null, null, 0, 0);
			}

			@Override
			public void func_219210_a(Entity p_219210_1_) {
				super.func_219210_a(p_219210_1_);
			}
		}
		DerivedTracker tracker = mock(DerivedTracker.class);
		// We bypass the constructor, so this needs to be manually set
		Int2ObjectMap<?> trackedEntities = new Int2ObjectOpenHashMap<>();
		ReflectionUtils.findAndSetPrivateField(tracker, ChunkManager.class, Int2ObjectMap.class, trackedEntities);
		ReflectionUtils.findAndSetPrivateField(tracker, ChunkManager.class, ServerWorld.class, world);
		doCallRealMethod().when(tracker).func_219210_a(any());

		Entity entity = type.create(world);

		int expectedDistance = StandardEntityManagers.VANILLA.getTrackDistance(identifier, entity);

		tracker.func_219210_a(entity);
		assertThat(trackedEntities, hasKey(entity.getEntityId()));
		Object entityTrackerEntry = trackedEntities.get(entity.getEntityId());
		Field actualDistanceField = ReflectionUtils.findField(entityTrackerEntry.getClass(), int.class);
		int actualDistance = actualDistanceField.getInt(entityTrackerEntry);

		assertEquals(expectedDistance, actualDistance);

		tracker.close();
		world.close();
	}
}
