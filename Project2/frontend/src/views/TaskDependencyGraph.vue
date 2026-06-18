<template>
  <div class="dag-page">
    <AppPageHeader :show-nav="false">
      <template #title>
        <button type="button" class="page-back" @click="goBack">返回</button>
        <h2 class="page-title">{{ pageTitle }}</h2>
      </template>
    </AppPageHeader>
    <div class="graph-layout">
      <div ref="containerRef" class="dag-container">
        <button
          type="button"
          class="refresh-btn refresh-btn--canvas"
          :disabled="loading"
          @click="initGraph"
        >
          刷新
        </button>
      </div>
      <aside class="legend" aria-label="任务状态图例">
        <h3>状态图例</h3>
        <div class="legend-item">
          <span class="legend-dot legend-dot--done"></span>
          <span>DONE</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot legend-dot--progress"></span>
          <span>IN_PROGRESS</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot legend-dot--todo"></span>
          <span>TODO</span>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Graph, type ElementDatum, type GraphData, type IElementEvent } from '@antv/g6'
import { getTaskDependencyGraph, getTeamTaskDependencyGraph } from '@/api/task'
import AppPageHeader from '@/components/common/AppPageHeader.vue'

type TaskDependencyNode = {
  id: number
  title: string
  status: 'TODO' | 'IN_PROGRESS' | 'DONE'
}

type TaskDependencyEdge = {
  from: number
  to: number
}

type TaskDependencyGraphData = {
  nodes?: TaskDependencyNode[]
  edges?: TaskDependencyEdge[]
}

let graph: Graph | null = null
const route = useRoute()
const router = useRouter()
const containerRef = ref<HTMLDivElement | null>(null)
const loading = ref(false)
const teamId = computed(() => Number(route.params.teamId))
const isTeamGraph = computed(() => Number.isFinite(teamId.value) && teamId.value > 0)
const pageTitle = computed(() => isTeamGraph.value ? '团队任务依赖图谱' : '个人任务依赖图谱')

// 初始化DAG图谱
const initGraph = async () => {
  loading.value = true
  try {
    const res = isTeamGraph.value
      ? await getTeamTaskDependencyGraph(teamId.value)
      : await getTaskDependencyGraph()
    const rawData = (res.data || { nodes: [], edges: [] }) as TaskDependencyGraphData

    // 格式化数据 + 状态染色
    const graphData: GraphData = {
      nodes: (rawData.nodes || []).map((node) => ({
        id: String(node.id),
        data: {
          title: node.title,
          status: node.status,
        },
      // 状态样式：未完成依赖=红色，进行中=蓝色，已完成=绿色
        style: {
          fill: node.status === 'DONE'
            ? '#3f9f67'
            : node.status === 'IN_PROGRESS'
              ? '#3f75d6'
              : '#d95f59',
          labelText: `#${node.id}`,
        },
      })),
      edges: (rawData.edges || []).map((edge) => ({
        source: String(edge.from),
        target: String(edge.to),
      })),
    }

    // 渲染图
    if (graph) graph.destroy()
    if (!containerRef.value) return

    graph = new Graph({
      container: containerRef.value,
      data: graphData,
      width: containerRef.value.clientWidth,
      height: 600,
      animation: false,
      layout: { type: 'dagre', rankdir: 'LR', nodesep: 50, ranksep: 100 },
      node: {
        type: 'circle',
        style: {
          size: 62,
          labelFill: '#fff',
          labelFontSize: 13,
          labelFontWeight: 700,
          labelPlacement: 'center',
          labelTextAlign: 'center',
          labelTextBaseline: 'middle',
          stroke: '#fff',
          lineWidth: 2,
          shadowColor: 'rgba(32, 48, 50, 0.18)',
          shadowBlur: 10,
        },
      },
      edge: {
        type: 'cubic-horizontal',
        style: {
          stroke: '#8da2b5',
          lineWidth: 2,
          endArrow: true,
          endArrowSize: 9,
        },
      },
      plugins: [
        {
          type: 'tooltip',
          trigger: 'hover',
          enable: (_event: IElementEvent, items: ElementDatum[]) => items.length > 0,
          getContent: async (_event: IElementEvent, items: ElementDatum[]) => {
            const data = items[0]?.data as { title?: string; status?: string } | undefined
            if (!data?.title) return ''
            return `<div class="graph-tooltip"><strong>${escapeHtml(data.title)}</strong><span>${data.status || ''}</span></div>`
          },
          onOpenChange: () => {},
        },
      ],
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    })

    await graph.render()
    await graph.fitView({ when: 'always' }, false)
    const currentZoom = graph.getZoom()
    if (Number.isFinite(currentZoom)) {
      await graph.zoomTo(currentZoom * 0.75, false, graph.getCanvasCenter())
    }
  } catch (err) {
    console.error('DAG渲染失败', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initGraph()
  window.addEventListener('resize', resizeGraph)
})

onUnmounted(() => {
  graph?.destroy()
  graph = null
  window.removeEventListener('resize', resizeGraph)
})

const resizeGraph = () => {
  if (!graph || !containerRef.value) return
  graph.resize(containerRef.value.clientWidth, 600)
}

const goBack = () => {
  if (isTeamGraph.value) {
    void router.push({ name: 'team-space', params: { teamId: String(teamId.value) } })
    return
  }
  void router.push({ name: 'tasks' })
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}
</script>

<style scoped>
.dag-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-back {
  min-height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 700;
  cursor: pointer;
}

.page-title {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2rem);
  color: #203032;
}

.refresh-btn {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  font-weight: 700;
  cursor: pointer;
}

.refresh-btn {
  color: var(--color-primary);
  background: #fff;
}

.refresh-btn:disabled {
  opacity: 0.62;
  cursor: not-allowed;
}

.graph-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 16px;
  align-items: start;
}

.dag-container {
  min-height: 600px;
  position: relative;
  border: 1px solid #dfe8e5;
  border-radius: 8px;
  background:
    linear-gradient(rgba(47, 107, 91, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(47, 107, 91, 0.04) 1px, transparent 1px),
    #fbfdfc;
  background-size: 24px 24px;
}

.refresh-btn--canvas {
  position: absolute;
  top: 12px;
  right: 12px;
  min-height: 32px;
  padding: 0 12px;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  z-index: 2;
  box-shadow: 0 10px 20px rgba(31, 45, 47, 0.12);
}

.legend {
  padding: 14px;
  border: 1px solid #dfe8e5;
  border-radius: 8px;
  background: #fbfdfc;
}

.legend h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #203032;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 10px;
  color: #3b4a49;
  font-size: 13px;
  font-weight: 700;
}

.legend-dot {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(32, 48, 50, 0.12);
}

.legend-dot--done {
  background: #3f9f67;
}

.legend-dot--progress {
  background: #3f75d6;
}

.legend-dot--todo {
  background: #d95f59;
}

:global(.graph-tooltip) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 260px;
  color: #203032;
}

:global(.graph-tooltip strong) {
  word-break: break-word;
}

:global(.graph-tooltip span) {
  color: #66706f;
  font-size: 12px;
}

@media (max-width: 640px) {
  .refresh-btn {
    width: 100%;
  }

  .graph-layout {
    grid-template-columns: 1fr;
  }
}
</style>
