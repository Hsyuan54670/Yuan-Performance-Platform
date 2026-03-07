import LazyEChart from "./LazyEChart";

export interface PerformanceChartSeries {
  name: string;
  color: string;
  data: number[];
}

interface PerformanceChartProps {
  title: string;
  xAxis: string[];
  series: PerformanceChartSeries[];
  yAxisName?: string;
  smooth?: boolean;
  area?: boolean;
  height?: number;
}

function PerformanceChart({
  title,
  xAxis,
  series,
  yAxisName,
  smooth = true,
  area = false,
  height = 280
}: PerformanceChartProps) {
  const option = {
    title: {
      text: title,
      textStyle: { fontSize: 15, fontWeight: 700, color: "#264653" }
    },
    tooltip: { trigger: "axis" },
    legend: { top: 4 },
    grid: { left: 20, right: 16, top: 52, bottom: 18, containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: xAxis,
      axisLine: { lineStyle: { color: "#8fa7b3" } }
    },
    yAxis: {
      type: "value",
      name: yAxisName,
      splitLine: { lineStyle: { type: "dashed", color: "#dde8ec" } }
    },
    series: series.map((s) => ({
      name: s.name,
      type: "line",
      smooth,
      showSymbol: false,
      data: s.data,
      lineStyle: { color: s.color, width: 2.5 },
      areaStyle: area ? { opacity: 0.12, color: s.color } : undefined
    }))
  };

  return <LazyEChart option={option} style={{ height }} />;
}

export default PerformanceChart;
