/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import androidx.paging.ActiveFlowTracker.FlowType
import androidx.paging.ActiveFlowTracker.FlowType.PAGED_DATA_FLOW
import androidx.paging.ActiveFlowTracker.FlowType.PAGE_EVENT_FLOW
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger

@RunWith(JUnit4::class)
class CachingTest {
    private val testScope = TestCoroutineScope()

    private val tracker = ActiveFlowTrackerImpl()
    @After
    fun checkResources() {
        testScope.cleanupTestCoroutines()
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
    }

    @Test
    fun noSharing() = testScope.runBlockingTest {
        val pageFlow = buildPageFlow()
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            )
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 1,
                generation = 0,
                start = 0,
                size = 9
            )
        )
    }

    @Test
    fun cached() = testScope.runBlockingTest {
        val pageFlow = buildPageFlow().cachedIn(testScope, tracker)
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            )
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            )
        )
    }

    @Test
    fun cached_afterMapping() = testScope.runBlockingTest {
        var mappingCnt = 0
        val pageFlow = buildPageFlow().map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = mappingIndex.toString())
            }
        }.cachedIn(testScope, tracker)
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            ) {
                it.copy(metadata = "0")
            }
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            ) {
                it.copy(metadata = "0")
            }
        )
    }

    @Test
    fun cached_beforeMapping() = testScope.runBlockingTest {
        var mappingCnt = 0
        val pageFlow = buildPageFlow().cachedIn(testScope, tracker).map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = mappingIndex.toString())
            }
        }
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            ) {
                it.copy(metadata = "0")
            }
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            ) {
                it.copy(metadata = "1")
            }
        )
    }

    @Test
    fun cached_afterMapping_withMoreMappingAfterwards() = testScope
        .runBlockingTest {
            var mappingCnt = 0
            val pageFlow = buildPageFlow().map { pagingData ->
                val mappingIndex = mappingCnt++
                pagingData.map {
                    it.copy(metadata = mappingIndex.toString())
                }
            }.cachedIn(testScope, tracker).map { pagingData ->
                val mappingIndex = mappingCnt++
                pagingData.map {
                    it.copy(metadata = "${it.metadata}_$mappingIndex")
                }
            }
            val firstCollect = pageFlow.collectItemsUntilSize(6)
            val secondCollect = pageFlow.collectItemsUntilSize(9)
            assertThat(firstCollect).isEqualTo(
                buildItems(
                    version = 0,
                    generation = 0,
                    start = 0,
                    size = 6
                ) {
                    it.copy(metadata = "0_1")
                }
            )

            assertThat(secondCollect).isEqualTo(
                buildItems(
                    version = 0,
                    generation = 0,
                    start = 0,
                    size = 9
                ) {
                    it.copy(metadata = "0_2")
                }
            )
        }

    @Test
    fun pagesAreClosedProperty() {
        val job = SupervisorJob()
        val subScope = CoroutineScope(job + Dispatchers.Default)
        val pageFlow = buildPageFlow().cachedIn(subScope, tracker)
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(0)
        val items = runBlocking {
            pageFlow.collectItemsUntilSize(9) {
                // see https://b/146676984
                delay(10)
            }
        }
        val firstList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 9
        )
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
        assertThat(items).isEqualTo(firstList)
        runBlocking {
            job.cancelAndJoin()
        }
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(0)
    }

    @Test
    fun cachedWithPassiveCollector() = testScope.runBlockingTest {
        val flow = buildPageFlow().cachedIn(testScope, tracker)
        val passive = ItemCollector(flow)
        passive.collectIn(testScope)
        testScope.runCurrent()
        // collecting on the paged source will trigger initial page
        assertThat(passive.items()).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 3
            )
        )
        val firstList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 9
        )
        // another collector is causing more items to be loaded, they should be reflected in the
        // passive one
        assertThat(flow.collectItemsUntilSize(9)).isEqualTo(firstList)
        assertThat(passive.items()).isEqualTo(firstList)
        val passive2 = ItemCollector(flow)
        passive2.collectIn(testScope)
        testScope.runCurrent()
        // a new passive one should receive all existing items immediately
        assertThat(passive2.items()).isEqualTo(firstList)

        // now we get another collector that'll fetch more pages, it should reflect in passives
        val secondList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 12
        )
        // another collector is causing more items to be loaded, they should be reflected in the
        // passive one
        assertThat(flow.collectItemsUntilSize(12)).isEqualTo(secondList)
        assertThat(passive.items()).isEqualTo(secondList)
        assertThat(passive2.items()).isEqualTo(secondList)
    }

    private fun buildPageFlow(): Flow<PagingData<Item>> {
        return PagingDataFlow(
            pagingSourceFactory = StringPagingSource.VersionedFactory()::create,
            config = PagingConfig(
                pageSize = 3,
                prefetchDistance = 1,
                enablePlaceholders = false,
                initialLoadSize = 3,
                maxSize = 1000
            )
        )
    }

    private suspend fun Flow<PagingData<Item>>.collectItemsUntilSize(
        expectedSize: Int,
        onEach: (suspend () -> Unit)? = null
    ): List<Item> {
        val pageData = this.first()
        val items = mutableListOf<Item>()
        val receiver = pageData.receiver
        var loadedPageCount = 0
        pageData.flow.filterIsInstance<PageEvent.Insert<Item>>()
            .onEach {
                onEach?.invoke()
            }
            .onEach {
                items.addAll(it.pages.flatMap {
                    it.data
                })
                loadedPageCount += it.pages.size
                if (items.size < expectedSize) {
                    receiver.addHint(
                        ViewportHint(
                            sourcePageIndex = loadedPageCount - 1,
                            indexInPage = it.pages.last().data.size - 1
                        )
                    )
                } else {
                    throw AbortCollectionException()
                }
            }.catch {
            }.toList()
        return items
    }

    /**
     * Paged list collector that does not call any hints but always collects
     */
    private class ItemCollector(
        val source: Flow<PagingData<Item>>
    ) {
        private var items: List<Item> = emptyList()
        private var job: Job? = null
        fun collectIn(scope: CoroutineScope) {
            check(job == null) {
                "don't call collect twice"
            }
            job = scope.launch {
                collectPassively()
            }
        }

        private suspend fun collectPassively() {
            source.collect {
                // clear to latest
                val list = mutableListOf<Item>()
                items = list
                it.flow.filterIsInstance<PageEvent.Insert<Item>>().collect {
                    it.pages.forEach {
                        list.addAll(it.data)
                    }
                }
            }
        }

        fun items() = items.toList()
    }

    private class StringPagingSource(
        private val version: Int
    ) : PagingSource<Int, Item>() {
        private var generation = -1

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
            when (params.loadType) {
                LoadType.REFRESH -> {
                    generation++
                    return doLoad(
                        position = params.key ?: 0,
                        size = params.loadSize
                    )
                }
                LoadType.START -> {
                    val loadSize = minOf(params.key!!, params.pageSize)
                    return doLoad(
                        position = params.key!! - params.loadSize,
                        size = loadSize
                    )
                }
                LoadType.END -> {
                    return doLoad(
                        position = params.key!!,
                        size = params.loadSize
                    )
                }
            }
        }

        private fun doLoad(
            position: Int,
            size: Int
        ): LoadResult<Int, Item> {
            return LoadResult.Page(
                data = buildItems(
                    version = version,
                    generation = generation,
                    start = position,
                    size = size
                ),
                prevKey = if (position == 0) null else position,
                nextKey = position + size
            )
        }

        class VersionedFactory {
            private var version = 0
            fun create() = StringPagingSource(version++)
        }
    }

    companion object {
        private fun buildItems(
            version: Int,
            generation: Int,
            start: Int,
            size: Int,
            modifier: ((Item) -> Item)? = null
        ): List<Item> {
            return (start until start + size).map { id ->
                Item(
                    pagingSourceId = version,
                    generation = generation,
                    value = id
                ).let {
                    modifier?.invoke(it) ?: it
                }
            }
        }
    }

    private data class Item(
        /**
         * which paged source generated this item
         */
        val pagingSourceId: Int,
        /**
         * # of refresh counts in the paged source
         */
        val generation: Int,
        /**
         * Item unique identifier
         */
        val value: Int,

        /**
         * Any additional data by transformations etc
         */
        val metadata: String? = null
    )

    private class ActiveFlowTrackerImpl : ActiveFlowTracker {
        private val counters = mapOf(
            PAGED_DATA_FLOW to AtomicInteger(0),
            PAGE_EVENT_FLOW to AtomicInteger(0)
        )

        override suspend fun onStart(flowType: FlowType) {
            (counters[flowType] ?: error("invalid type $flowType")).incrementAndGet()
        }

        override suspend fun onComplete(flowType: FlowType) {
            (counters[flowType] ?: error("invalid type $flowType")).decrementAndGet()
        }

        fun pageDataFlowCount() = (counters[PAGED_DATA_FLOW] ?: error("unexpected")).get()
        fun pageEventFlowCount() = (counters[PAGE_EVENT_FLOW] ?: error("unexpected")).get()
    }

    private class AbortCollectionException : Throwable()
}