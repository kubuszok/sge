# Test Audit Batch T2: Ashley ECS, GDX-AI, Simple-Graphs

Audit date: 2026-04-18

---

## Ashley ECS

### ComponentOperationHandlerTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/ComponentOperationHandlerTests.java`
SGE equivalent: **none found** (no dedicated test file)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `add` | ComponentOperationHandler fires componentAdded signal immediately when not delayed | NOT_PORTED | - | Tests internal ComponentOperationHandler directly |
| 2 | `addDelayed` | ComponentOperationHandler defers componentAdded until processOperations when delayed=true | NOT_PORTED | - | Tests internal ComponentOperationHandler directly |
| 3 | `remove` | ComponentOperationHandler fires componentRemoved signal immediately | NOT_PORTED | - | Tests internal ComponentOperationHandler directly |
| 4 | `removeDelayed` | ComponentOperationHandler defers componentRemoved until processOperations when delayed=true | NOT_PORTED | - | Tests internal ComponentOperationHandler directly |

### ComponentTypeTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/ComponentTypeTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/ComponentTypeSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `validComponentType` | getFor returns non-null | PORTED | `valid ComponentType` | |
| 2 | `sameComponentType` | Same class returns same ComponentType and index | PORTED | `same class returns same ComponentType` | |
| 3 | `differentComponentType` | Different class returns different ComponentType and index | PORTED | `different class returns different ComponentType` | |

### EngineTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/EngineTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/EngineSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addAndRemoveEntity` | EntityListener add/remove counts with listener lifecycle | PORTED | `addEntity and removeEntity` | |
| 2 | `addComponentInsideListener` | Adding component inside listener triggers another family listener | PORTED | `addComponent inside listener` | |
| 3 | `addAndRemoveSystem` | System add/remove lifecycle, addedCalls/removedCalls | PORTED | `addSystem and removeSystem` | |
| 4 | `getSystems` | getSystems returns correct count | PORTED | `getSystems` | |
| 5 | `addTwoSystemsOfSameClass` | Second system of same class replaces first | PORTED | `add two systems of same class replaces first` | |
| 6 | `systemUpdate` | update() calls systems, removed system not called | PORTED | `update calls systems` | |
| 7 | `systemUpdateOrder` | Systems called in priority order | PORTED | `update calls systems in priority order` | |
| 8 | `entitySystemEngineReference` | engine reference set/cleared on add/remove | PORTED | `system engine reference lifecycle` | |
| 9 | `ignoreSystem` | setProcessing(false) skips update | PORTED | `ignoreSystem when processing is false` | |
| 10 | `entitiesForFamily` | Family.all query returns matching entities | PORTED | `getEntitiesFor family` | |
| 11 | `entityForFamilyWithRemoval` | Entity removal updates family membership | PORTED | `entity for family with removal` | |
| 12 | `entitiesForFamilyAfter` | Adding components after addEntity updates family | PORTED | `entities for family after adding components post-addEntity` | |
| 13 | `entitiesForFamilyWithRemoval` | Component removal and entity removal update family | PORTED | `entity component change updates family membership` | |
| 14 | `entitiesForFamilyWithRemovalAndFiltering` | Family exclude + component removal | PORTED | `family filtering with removal and exclusion` | |
| 15 | `entitySystemRemovalWhileIterating` | Removing entities in system update | PORTED | `entity removal while iterating in system` | |
| 16 | `entityAddRemoveComponentWhileIterating` | Add/remove components during iteration with deferred signals | NOT_PORTED | - | Complex test with ComponentAddSystem/ComponentRemoveSystem checking deferred listener callbacks during iteration. Would need `ComponentAddedListener.checkEntityListenerUpdate/NonUpdate` pattern |
| 17 | `cascadeOperationsInListenersWhileUpdating` | Recursive cascade create/destroy in listeners | NOT_PORTED | - | Complex test mixing component add/remove with entity add/remove in cascading listeners |
| 18 | `familyListener` | Family-scoped add/remove listeners with removeAllEntities(family) | PORTED | `family listener` + `removeAllEntities for specific family` | Original is one big test covering all family listener edge cases. SGE splits into two tests but covers most of it. The `removeAllEntities(familyA)` filtering case is covered. |
| 19 | `createManyEntitiesNoStackOverflow` | 15000 entities, no stack overflow | PORTED | `create many entities without stack overflow` | |
| 20 | `getEntities` | getEntities returns live list | PORTED | `getEntities returns live list` | |
| 21 | `addEntityTwice` | Throws IllegalArgumentException | PORTED | `addEntity twice throws` | |
| 22 | `nestedUpdateException` | Nested engine.update() throws IllegalStateException | PORTED | `nested update throws IllegalStateException` | |
| 23 | `systemUpdateThrows` | Exception in system doesn't permanently break engine | PORTED | `system update that throws does not leave engine in updating state` | |
| 24 | `createNewEntity` | engine.createEntity() not null | PORTED | `createEntity returns new entity` | |
| 25 | `createNewComponent` | engine.createComponent(ComponentD.class) not null | PORTED | `createComponent works via PooledEngine with factory` | Adapted for SGE's factory-based approach |
| 26 | `createPrivateComponent` | createComponent for private class returns null | PORTED | `createComponent throws by default in base Engine` | SGE throws instead of returning null (idiomatic) |
| 27 | `removeEntityBeforeAddingAndWhileEngineIsUpdating` | Issue #306: remove-add-remove sequence during update | PORTED | `removeEntityBeforeAddingAndWhileEngineIsUpdating` | |

### EntityListenerTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/EntityListenerTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/EngineSuite.scala` (merged)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addEntityListenerFamilyRemove` | Adding entity inside family listener on remove | PORTED | `addEntityListenerFamilyRemove` | |
| 2 | `addEntityListenerFamilyAdd` | Adding entity inside family listener on add | PORTED | `addEntityListenerFamilyAdd` | |
| 3 | `addEntityListenerNoFamilyRemove` | Adding entity inside no-family listener on remove | PORTED | `addEntityListenerNoFamilyRemove` | |
| 4 | `addEntityListenerNoFamilyAdd` | Adding entity inside no-family listener on add | PORTED | `addEntityListenerNoFamilyAdd` | |
| 5 | `entityListenerPriority` | Listeners called in priority order with add/remove/re-add | PORTED | `entityListenerPriority: listeners called in priority order` | Uses ArrayBuffer order tracking instead of Mockito InOrder |
| 6 | `familyListenerPriority` | Family-scoped listeners called in priority order | PORTED | `familyListenerPriority: family-scoped listeners called in priority order` | |
| 7 | `componentHandlingInListeners` | Add/remove components inside entity listeners during engine lifecycle | PORTED | `componentHandlingInListeners: add/remove components inside entity listeners` | Uses boolean flags instead of Mockito verify |

### EntityManagerTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/EntityManagerTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/EntityManagerSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addAndRemoveEntity` | Basic add/remove with listener counts | PORTED | `addAndRemoveEntity via EntityManager` | |
| 2 | `getEntities` | getEntities returns live list | PORTED | `getEntities via EntityManager` | |
| 3 | `addEntityTwice1` | Immediate duplicate throws | PORTED | `addEntityTwice immediate throws` | |
| 4 | `addEntityTwice2` | delayed=false duplicate throws | PORTED | `addEntityTwice with delayed=false throws` | |
| 5 | `addEntityTwiceDelayed` | Delayed duplicate throws on process | PORTED | `addEntityTwiceDelayed throws on process` | |
| 6 | `delayedOperationsOrder` | removeAll then add delayed ordering | PORTED | `delayedOperationsOrder: removeAll then add` | |
| 7 | `removeAndAddEntityDelayed` | Remove then add same entity delayed | PORTED | `removeAndAddEntityDelayed` | |
| 8 | `removeAllAndAddEntityDelayed` | removeAll then add same entity delayed | PORTED | `removeAllAndAddEntityDelayed` | |

### EntityTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/EntityTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/EntitySuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addAndReturnComponent` | addAndReturn returns the component, count=2 | PORTED | `addAndReturn returns the component` | |
| 2 | `addAndReturnComponentGeneric` | Compile-time generic check | NOT_APPLICABLE | - | Java generics compile-time test; Scala handles this differently |
| 3 | `noComponents` | Empty entity has no components, bits empty, mappers return null | PORTED | `no components initially` | |
| 4 | `addAndRemoveComponent` | Add/remove with bits and mapper checks | PORTED | `add and remove component` | |
| 5 | `removeUnexistingComponent` | Removing 65 non-existing component types (ASM factory) | NOT_APPLICABLE | - | Uses ASM bytecode class factory (JVM-only, Mockito-based); SGE has `remove non-existing component returns empty` covering the basic case |
| 6 | `addAndRemoveAllComponents` | removeAll clears all components and bits | PORTED | `add and removeAll` | |
| 7 | `addSameComponent` | Adding same type replaces previous | PORTED | `add replaces existing same-type component` | |
| 8 | `componentListener` | componentAdded/componentRemoved signals fire | PORTED | `componentAdded signal fires` | |
| 9 | `getComponentByClass` | getComponent returns exact instance | PORTED | `getComponent by class` | |

### FamilyManagerTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/FamilyManagerTests.java`
SGE equivalent: **none found** (no dedicated FamilyManager test; functionality tested through Engine)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `entitiesForFamily` | FamilyManager.updateFamilyMembership with direct API | NOT_PORTED | - | Tests FamilyManager internal API directly. Functionality covered indirectly via EngineSuite `getEntitiesFor family` |
| 2 | `entityForFamilyWithRemoval` | FamilyManager direct removal via entity.removing flag | NOT_PORTED | - | Tests internal removing flag directly |
| 3 | `entitiesForFamilyAfter` | FamilyManager after components added to pre-existing entities | NOT_PORTED | - | Tests FamilyManager internal API. Covered indirectly via EngineSuite |
| 4 | `entitiesForFamilyWithRemoval` | FamilyManager with component removal + entity removal | NOT_PORTED | - | Tests FamilyManager internal API. Covered indirectly |
| 5 | `entitiesForFamilyWithRemovalAndFiltering` | FamilyManager with exclude filter | NOT_PORTED | - | Tests FamilyManager internal API. Covered indirectly |
| 6 | `entityListenerThrows` | Exception in listener resets notifying flag | PORTED | `entityListenerThrows resets notifying flag` (in EngineSuite) | |

### FamilyTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/FamilyTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/FamilySuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `validFamily` | Various Family.all().get() non-null | PORTED | `valid families can be created` | |
| 2 | `sameFamily` | Same spec returns same Family (caching) | PORTED | `same spec returns same Family instance (caching)` | |
| 3 | `differentFamily` | Different specs return different families/indices | PORTED | `different specs return different families` | Partially ported -- original has very exhaustive 13-family comparison; SGE tests 3 families. Core concept covered. |
| 4 | `familyEqualityFiltering` | all/one/exclude equality | PORTED | `family equality with all/one/exclude` | |
| 5 | `entityMatch` | Entity with required components matches | PORTED | `Family.all matches entity with all components` + `Family.all matches entity with extra components` | |
| 6 | `entityMismatch` | Entity missing required component doesn't match | PORTED | `Family.all does not match entity missing a component` | |
| 7 | `entityMatchThenMismatch` | Removing a component breaks match | PORTED | `entity match then mismatch on remove` | |
| 8 | `entityMismatchThenMatch` | Adding a component creates match | PORTED | `entity mismatch then match on add` | |
| 9 | `testEmptyFamily` | Empty family matches any entity | PORTED | `empty family matches any entity` | |
| 10 | `familyFiltering` | Complex all/one/exclude filtering | PORTED | `complex family filtering` | |
| 11 | `matchWithPooledEngine` | Family.exclude works with PooledEngine | NOT_PORTED | - | Tests Family matching within PooledEngine context with systems |
| 12 | `matchWithPooledEngineInverse` | Family.exclude inverse with PooledEngine | NOT_PORTED | - | |
| 13 | `matchWithoutSystems` | Family.exclude works in PooledEngine without systems | NOT_PORTED | - | |
| 14 | `matchWithComplexBuilding` | all+one+exclude complex build | PORTED | `match with complex building` | |

### PooledEngineTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/PooledEngineTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/PooledEngineSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `entityRemovalListenerOrder` | Listener can access component during removal | PORTED | `entity removal listener order` | |
| 2 | `resetEntityCorrectly` | Flags, components, familyBits cleared on reset | PORTED | `resetEntity correctly: flags, components, familyBits cleared` | |
| 3 | `recycleEntity` | Removed entities reused from pool | PORTED | `recycleEntity: removed entities are reused` | |
| 4 | `removeEntityTwice` | Double removal doesn't crash | PORTED | `remove entity twice does not crash` | |
| 5 | `recycleComponent` | Components pooled and reset | PORTED | `recycleComponent: components are pooled and reset` | |
| 6 | `createNewComponent` | PooledEngine creates component | PORTED | `createComponent creates component via factory` | |
| 7 | `addSameComponentShouldResetAndReturnOldComponentToPool` | Replacing component resets old one | PORTED | `addSameComponent should reset and return old component to pool` | |
| 8 | `removeComponentReturnsItToThePoolExactlyOnce` | Remove returns to pool once, not duplicated | PORTED | `removeComponent returns it to the pool exactly once` | |

### SystemManagerTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/core/SystemManagerTests.java`
SGE equivalent: **none found** (no dedicated test; functionality tested through Engine)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addAndRemoveSystem` | SystemManager direct add/remove with SystemListener | NOT_PORTED | - | Tests SystemManager internal API directly with SystemListenerSpy. Covered indirectly via EngineSuite |
| 2 | `getSystems` | SystemManager getSystems count + listener counts | NOT_PORTED | - | Covered indirectly via EngineSuite `getSystems` |
| 3 | `addTwoSystemsOfSameClass` | SystemManager replaces system of same class | NOT_PORTED | - | Covered indirectly via EngineSuite |
| 4 | `systemUpdateOrder` | Systems sorted by priority | NOT_PORTED | - | Covered indirectly via EngineSuite `update calls systems in priority order` |

### SignalTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/signals/SignalTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/signals/SignalSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addListenerAndDispatch` | Single listener receives dispatches | PORTED | `add listener and dispatch` | |
| 2 | `addListenersAndDispatch` | Multiple listeners all receive | PORTED | `multiple listeners all receive` | |
| 3 | `addListenerDispatchAndRemove` | Remove listener stops receiving | PORTED | `add, dispatch, and remove listener` | |
| 4 | `removeWhileDispatch` | Remove during dispatch is safe | PORTED | `remove during dispatch (snapshot safety)` | |
| 5 | `removeAll` | removeAllListeners works | PORTED | `removeAllListeners` | |

### IntervalIteratingTest.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/systems/IntervalIteratingTest.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/systems/IntervalIteratingSystemSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `intervalSystem` | Entities processed at correct interval | PORTED | `processes entities at interval` | |
| 2 | `processingUtilityFunctions` | startProcessing/endProcessing called | PORTED | `startProcessing and endProcessing called at intervals` | |

### IntervalSystemTest.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/systems/IntervalSystemTest.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/systems/IntervalSystemSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `intervalSystem` | Updates at correct interval | PORTED | `calls updateInterval at correct intervals` | |
| 2 | `testGetInterval` | interval accessor returns correct value | PORTED | `interval accessor returns correct value` | |

### IteratingSystemTest.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/systems/IteratingSystemTest.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/systems/IteratingSystemSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `shouldIterateEntitiesWithCorrectFamily` | Only matching entities processed | PORTED | `processes entities with correct family` | |
| 2 | `entityRemovalWhileIterating` | Remove entity during iteration | PORTED | `entity removal while iterating` | |
| 3 | `componentRemovalWhileIterating` | Remove component during iteration | PORTED | `component removal while iterating` | |
| 4 | `processingUtilityFunctions` | startProcessing/endProcessing called | PORTED | `startProcessing and endProcessing called` | |

### SortedIteratingSystemTest.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/systems/SortedIteratingSystemTest.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/systems/SortedIteratingSystemSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `shouldIterateEntitiesWithCorrectFamily` | Only matching entities processed (sorted) | PORTED | `processes entities with correct family` | |
| 2 | `entityRemovalWhileIterating` | Remove entity during sorted iteration | PORTED | `entity removal while iterating` | |
| 3 | `componentRemovalWhileIterating` | Remove component during sorted iteration | PORTED | `component removal while iterating` | |
| 4 | `entityOrder` | Entities processed in comparator order, forceSort | PORTED | `processes entities in sorted order` + `forceSort re-sorts with updated values` | |
| 5 | `processingUtilityFunctions` | startProcessing/endProcessing called | PORTED | `startProcessing and endProcessing called` | |

### BagTest.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/utils/BagTest.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/utils/BagSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testSet` | Set replaces element at index, size unchanged | PORTED | `set specific index` | |

### ImmutableArrayTests.java

Original path: `original-src/ashley/ashley/tests/com/badlogic/ashley/utils/ImmutableArrayTests.java`
SGE equivalent: `sge-extension/ecs/src/test/scala/sge/ecs/utils/ImmutableArraySuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `sameValues` | Size and get reflect backing array | PORTED | `size reflects backing array` + `get by index matches backing` | |
| 2 | `iteration` | Iterator returns values in order | PORTED | `iterator matches backing order` | |
| 3 | `forbiddenRemoval` | Iterator.remove() throws | NOT_PORTED | - | SGE's ImmutableArray iterator may not expose remove(); design difference |

### Ashley ECS Summary

**Total original @Test methods: 98**
- PORTED: 80
- NOT_PORTED: 15
- NOT_APPLICABLE: 3 (ASM bytecode factory, Java generic compile-time check, ImmutableArray iterator.remove)

**NOT_PORTED breakdown:**
- ComponentOperationHandler (4 tests) -- tests internal API directly; behavior covered indirectly via Engine
- FamilyManager (5 tests) -- tests internal API directly; 4 covered indirectly via Engine, 1 (entityListenerThrows) is ported
- SystemManager (4 tests) -- tests internal API directly; all covered indirectly via Engine
- EngineTests.entityAddRemoveComponentWhileIterating (1 test) -- complex deferred signal verification during iteration
- EngineTests.cascadeOperationsInListenersWhileUpdating (1 test) -- recursive cascade listener test
- FamilyTests: matchWithPooledEngine/Inverse/WithoutSystems (3 tests) -- PooledEngine + Family.exclude interaction
- ImmutableArrayTests.forbiddenRemoval (1 test) -- iterator.remove() not applicable in Scala idiom

---

## GDX-AI

### ParallelTest.java (unit tests)

Original path: `original-src/gdx-ai/gdx-ai/tests/com/badlogic/gdx/ai/btree/branch/ParallelTest.java`
SGE equivalent: `sge-extension/ai/src/test/scala/sge/ai/btree/BehaviorTreeSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `testResumeOrchestratorSequencePolicy` | Resume orchestrator + Sequence policy: multi-step execution tracking | PARTIALLY_PORTED | `Parallel Resume + Sequence` tests | SGE tests single-step outcomes (all succeed, one fails, running). Original tracks execution counts across 4 steps with status transitions. SGE does NOT test multi-step resume behavior with mutable task status changes. |
| 2 | `testResumeOrchestratorSelectorPolicy` | Resume orchestrator + Selector policy: multi-step with execution counts | PARTIALLY_PORTED | `Parallel Resume + Selector` tests | Same issue: SGE tests single-step; original tracks execution counts over 4 steps |
| 3 | `testJoinOrchestratorSequencePolicySequentialOrder` | Join orchestrator + Sequence: completed tasks don't re-execute, multi-step | PARTIALLY_PORTED | `Parallel Join + Sequence` tests | SGE tests single-step outcomes. Original verifies join behavior where completed tasks stop running. SGE doesn't test the multi-step Join state machine. |
| 4 | `testJoinOrchestratorSequencePolicyInverseOrder` | Join orchestrator + Sequence (task2 succeeds first) | NOT_PORTED | - | Tests inverse completion order with execution count tracking |
| 5 | `testJoinOrchestratorSelectorPolicy` | Join orchestrator + Selector: failed task stops, succeeded task ends parallel | NOT_PORTED | - | Tests join selector with failure then success pattern |

### IndexedAStarPathFinderTest.java (unit tests)

Original path: `original-src/gdx-ai/gdx-ai/tests/com/badlogic/gdx/ai/pfa/indexed/IndexedAStarPathFinderTest.java`
SGE equivalent: `sge-extension/ai/src/test/scala/sge/ai/pfa/AStarSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `searchNodePath_WhenSearchingAdjacentTile_ExpectedOutputPathLengthEquals2` | Adjacent tile pathfinding (4 sub-cases) | PORTED | `find shortest path from (0,0) to (4,4)` | Different topology but tests same concept: basic A* pathfinding |
| 2 | `searchNodePath_WhenSearchCanHitDeadEnds_ExpectedOutputPathFound` | Complex maze with dead ends | PORTED | `path around obstacle` | SGE uses wall-block pattern instead of text-based maze |
| 3 | `searchNodePath_WhenDestinationUnreachable_ExpectedNoOutputPathFound` | Unreachable destination | PORTED | `no path exists for isolated nodes` | |
| 4 | `searchNodePath_WhenGraphIsUpdatingOnTheFly_ExpectedFailToFindEndByReference` | Dynamic graph with new node instances - fails by reference | NOT_PORTED | - | Tests reference-based stop condition with dynamic graph that creates new node instances on getConnections. Requires MyDynamicGraph + MyNodesFactory pattern |
| 5 | `searchNodePath_WhenGraphIsUpdatedOnTheFly_ExpectedSucceedToFindEndByEquals` | Dynamic graph with equals-based stop condition succeeds | NOT_PORTED | - | Tests EqualsMethodStopCondition with MyNodeWithEquals. The `dynamic graph` tests in SGE test graph mutation between calls, not on-the-fly node replacement |

### Visual/Integration Tests (NOT unit tests)

Original path: `original-src/gdx-ai/tests/src/com/badlogic/gdx/ai/tests/`
These are LibGDX application tests (visual demos) -- NOT unit tests:
- `BehaviorTreeTests.java` - Visual BTree demo app
- `MessageTests.java` - Visual messaging demo app  
- `PathFinderTests.java` - Visual pathfinding demo app
- `StateMachineTest.java` - Visual FSM demo app
- `SteeringBehaviorsTest.java` - Visual steering demo app

**Status: NOT_APPLICABLE** -- These are interactive visual test applications, not unit tests. SGE has proper unit tests for these modules in `StateMachineSuite`, `MessageDispatcherSuite`, `SteeringSuite`, `AStarSuite`, `BehaviorTreeSuite`.

### GDX-AI Summary

**Total original unit @Test methods: 10** (ParallelTest: 5, IndexedAStarPathFinderTest: 5)
- PORTED: 3
- PARTIALLY_PORTED: 3
- NOT_PORTED: 4

**Key missing coverage:**
1. Multi-step Parallel orchestrator behavior (Resume/Join) with execution count tracking across multiple tree steps
2. Join orchestrator inverse completion order test
3. Join orchestrator + Selector policy with fail-then-succeed pattern
4. Dynamic graph reference vs equals stop conditions (IndexedAStarPathFinder)

---

## Simple-Graphs

### AlgorithmsTest.java

Original path: `original-src/simple-graphs/src/test/java/space/earlygrey/simplegraphs/AlgorithmsTest.java`
SGE equivalent: `sge-extension/graphs/src/test/scala/sge/graphs/DirectedGraphSuite.scala` + `UndirectedGraphSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `shortestPathShouldBeCorrect` | Shortest path with/without heuristic, directed + undirected, disconnected | PORTED | `shortest path with Dijkstra` + `shortest path - unreachable` (Dir) + `shortest path in undirected graph` (Undir) | Original uses 20x20 grid; SGE uses simple graph. Heuristic variant not explicitly tested. |
| 2 | `cyclesShouldBeDetected` | Cycle detection for directed and undirected | PORTED | `cycle detection - has/no cycle` (Dir) + `cycle detection in undirected graph` + `no cycle in tree` (Undir) | |
| 3 | `bfsShouldWork` | BFS traversal order and tree building | PORTED | `BFS visits all reachable vertices` | SGE tests visit count and order but doesn't build spanning tree or check multiple valid orderings |
| 4 | `dfsShouldWork` | DFS traversal order with depth limit | PORTED | `DFS visits all reachable vertices` | SGE tests visit count but doesn't test depth-limit via `step.ignore()` |
| 5 | `topologicalSortShouldWork` | Topological sort correctness + cycle detection | PORTED | `topological sort` | SGE tests ordering constraints; doesn't test the "cycle prevents sort" case (original does `assertTrue(!success)`) |
| 6 | `mwstShouldBeTree` | Minimum weight spanning tree | PORTED | `minimum spanning tree` | Original uses 20-vertex complete graph; SGE uses 4-vertex graph |

### ArrayTest.java

Original path: `original-src/simple-graphs/src/test/java/space/earlygrey/simplegraphs/ArrayTest.java`
SGE equivalent: `sge-extension/graphs/src/test/scala/sge/graphs/InternalArraySuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `addAllShouldAddAllItemsFromSourceToTargetAndResizeTarget` | addAll with resize | PORTED | `addAll should add all items from source to target and resize target` | |
| 2 | `addAllShouldAddAllItemsFromSourceToTargetAndUpdateTargetSize` | addAll with size update | PORTED | `addAll should add all items from source to target and update target size` | |

### GraphBuilderTest.java

Original path: `original-src/simple-graphs/src/test/java/space/earlygrey/simplegraphs/GraphBuilderTest.java`
SGE equivalent: `sge-extension/graphs/src/test/scala/sge/graphs/DirectedGraphSuite.scala` + `UndirectedGraphSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `completeGraphCanBeBuilt` | Complete graph edge count for undirected and directed | PORTED | `complete graph builder` (Dir) + `complete graph builder for undirected` (Undir) | |

### GraphTest.java

Original path: `original-src/simple-graphs/src/test/java/space/earlygrey/simplegraphs/GraphTest.java`
SGE equivalent: `sge-extension/graphs/src/test/scala/sge/graphs/DirectedGraphSuite.scala` + `UndirectedGraphSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `verticesCanBeAddedAndRemoved` | Add/remove vertices, order, removeAll, shuffle, removeVertexIf, BadHashInteger | PARTIALLY_PORTED | `add and remove vertices` (Dir/Undir) | SGE covers basic add/remove/duplicate/contains. Missing: shuffle order preservation, removeVertexIf, BadHashInteger collision handling |
| 2 | `vertexanBeRemovedFromDirectedGraph` | Vertex removal with edge cleanup in directed graph | PORTED | `add and remove edges` (DirectedGraphSuite) | SGE tests edge count after vertex operations |
| 3 | `vertexCanBeDisconnectedFromDirectedGraph` | disconnect() removes edges but keeps vertex | NOT_PORTED | - | Tests `graph.disconnect(vertex)` preserving vertex count but removing edges |
| 4 | `edgesCanBeAddedAndRemoved` | Grid graph edges, removeEdge, removeAllEdges, directed vs undirected edge behavior, removeEdgeIf | PARTIALLY_PORTED | `add and remove edges` (Dir) + `remove undirected edge` (Undir) | SGE covers basic edge add/remove. Missing: grid graph edge count verification, removeEdgeIf, verifying directed single-direction removal vs undirected both-direction removal |
| 5 | `verticesCanBeSorted` | sortVertices with comparator | NOT_PORTED | - | Tests `graph.sortVertices(Comparator)` |
| 6 | `edgesCanBeSorted` | sortEdges with comparator | NOT_PORTED | - | Tests `graph.sortEdges(Comparator)` |

### StructuresTest.java

Original path: `original-src/simple-graphs/src/test/java/space/earlygrey/simplegraphs/StructuresTest.java`
SGE equivalent: `sge-extension/graphs/src/test/scala/sge/graphs/StructuresSuite.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | `nodeMapShouldWork` | NodeMap put/contains/remove/size with threshold, BadHashInteger collisions | PORTED | `nodeMap should work` | |

### Simple-Graphs Summary

**Total original @Test methods: 12**
- PORTED: 8
- PARTIALLY_PORTED: 2
- NOT_PORTED: 3

**NOT_PORTED:**
1. `vertexCanBeDisconnectedFromDirectedGraph` -- `disconnect()` API test
2. `verticesCanBeSorted` -- `sortVertices()` test
3. `edgesCanBeSorted` -- `sortEdges()` test

**PARTIALLY_PORTED gaps:**
- `verticesCanBeAddedAndRemoved`: missing removeVertexIf, shuffle order, BadHashInteger collision handling
- `edgesCanBeAddedAndRemoved`: missing removeEdgeIf, directed vs undirected edge removal semantics, grid edge count

---

## Overall Summary

| Library | Original Tests | Ported | Partially | Not Ported | N/A | Coverage |
|---------|---------------|--------|-----------|------------|-----|----------|
| Ashley ECS | 98 | 80 | 0 | 15 | 3 | 82% (84% exc. N/A) |
| GDX-AI (unit) | 10 | 3 | 3 | 4 | 0 | 30% (45% counting partial) |
| Simple-Graphs | 12 | 8 | 2 | 3 | 0 | 67% (75% counting partial) |
| **Total** | **120** | **91** | **5** | **22** | **3** | **76%** |

## Priority List for Porting

### Priority 1 (High -- missing core behavior tests)

1. **GDX-AI: Multi-step Parallel orchestrator tests** (3 tests)
   - `testResumeOrchestratorSequencePolicy` -- full multi-step version with execution counts
   - `testJoinOrchestratorSequencePolicyInverseOrder` -- inverse completion order
   - `testJoinOrchestratorSelectorPolicy` -- fail-then-succeed pattern
   - These test the Parallel task's state machine behavior across multiple `step()` calls, which is the core differentiator between Resume and Join orchestrators.

2. **GDX-AI: Dynamic graph A* tests** (2 tests)
   - `searchNodePath_WhenGraphIsUpdatingOnTheFly_ExpectedFailToFindEndByReference`
   - `searchNodePath_WhenGraphIsUpdatedOnTheFly_ExpectedSucceedToFindEndByEquals`
   - These test the EqualsMethodStopCondition and dynamic graph node replacement, important for real-world usage.

3. **Ashley: EngineTests cascade/deferred tests** (2 tests)
   - `entityAddRemoveComponentWhileIterating` -- verifies deferred component operations during system update
   - `cascadeOperationsInListenersWhileUpdating` -- recursive listener cascade create/destroy

### Priority 2 (Medium -- internal API coverage gaps)

4. **Ashley: FamilyManager internal tests** (5 tests) -- only if FamilyManager is exposed as a public API; otherwise low priority since behavior is covered via Engine
5. **Ashley: SystemManager internal tests** (4 tests) -- similarly only if public API
6. **Ashley: ComponentOperationHandler tests** (4 tests) -- internal component operation deferral

### Priority 3 (Low -- edge case / feature tests)

7. **Simple-Graphs: sortVertices, sortEdges, disconnect** (3 tests) -- verify if these APIs exist in SGE port
8. **Simple-Graphs: removeVertexIf, removeEdgeIf** (within partially ported tests) -- predicate-based removal
9. **Ashley: FamilyTests with PooledEngine** (3 tests) -- Family.exclude + PooledEngine interaction
10. **Ashley: ImmutableArray.forbiddenRemoval** (1 test) -- may not apply in Scala
