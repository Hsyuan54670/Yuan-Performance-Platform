import type { CSSProperties } from "react";
import { useEffect, useRef } from "react";
import { LineChart, PieChart } from "echarts/charts";
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type TitleComponentOption,
  type TooltipComponentOption
} from "echarts/components";
import { init, use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import type { ComposeOption, EChartsType } from "echarts/core";
import type { LineSeriesOption, PieSeriesOption } from "echarts/charts";

type ECOption = ComposeOption<
  TitleComponentOption | TooltipComponentOption | LegendComponentOption | GridComponentOption | LineSeriesOption | PieSeriesOption
>;

use([TitleComponent, TooltipComponent, LegendComponent, GridComponent, LineChart, PieChart, CanvasRenderer]);

interface LazyEChartProps {
  option: ECOption | Record<string, unknown>;
  style?: CSSProperties;
}

function LazyEChart({ option, style }: LazyEChartProps) {
  const ref = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<EChartsType | null>(null);

  useEffect(() => {
    if (!ref.current) return;

    if (!chartRef.current) {
      chartRef.current = init(ref.current);
    }

    chartRef.current.setOption(option as ECOption, {
      notMerge: false,
      lazyUpdate: true
    });

    const onResize = () => chartRef.current?.resize();
    window.addEventListener("resize", onResize);

    return () => {
      window.removeEventListener("resize", onResize);
    };
  }, [option]);

  useEffect(
    () => () => {
      chartRef.current?.dispose();
      chartRef.current = null;
    },
    []
  );

  return <div ref={ref} style={{ width: "100%", minHeight: 180, ...style }} />;
}

export default LazyEChart;
