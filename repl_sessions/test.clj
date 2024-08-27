(ns test
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance]
           [net.minestom.server.entity Player]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance InstanceManager]
           [net.minestom.server.instance InstanceContainer]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Pos]
                                        ;noise
           [de.articdive.jnoise.pipeline JNoise]
           [de.articdive.jnoise.generators.noisegen.opensimplex FastSimplexNoiseGenerator]
           [de.articdive.jnoise.core.api.functions Interpolation]
           [de.articdive.jnoise.generators.noise_parameters.fade_functions FadeFunction]
           )
  )
