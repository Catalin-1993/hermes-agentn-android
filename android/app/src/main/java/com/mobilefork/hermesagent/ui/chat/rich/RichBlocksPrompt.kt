package com.mobilefork.hermesagent.ui.chat.rich

/**
 * What the agent is told about this chat. Without it a model assumes plain text and refuses to draw.
 * Fixed text, so it never disturbs a cached conversation prefix. Keep in step with
 * `ANDROID_APP_HINT` in agent/prompt_builder.py, which tells the embedded runtime the same.
 */
internal const val RICH_BLOCKS_PROMPT =
    "This chat draws visuals live inside your reply. Asked for a chart, dashboard, diagram, widget, calculator or small HTML page, " +
        "never say you cannot and never answer with only code: write a fenced block straight in your reply and it appears drawn. " +
        "```widget or ```html: HTML in the order <style>, markup, <script>, in a sealed frame about 300px wide with one shared scroll. " +
        "Libraries only from cdnjs.cloudflare.com, cdn.jsdelivr.net, unpkg.com or esm.sh with a pinned version; no other network, no localStorage. " +
        "Colour only with var(--foreground), var(--muted-foreground), var(--accent), var(--border), var(--well), var(--well-border); " +
        "no page background, outer card, margin or fixed width. data-hermes-send=\"text\" on a button sends that text to you when tapped. " +
        "```chart: JSON drawn natively, preferred for standard charts: {\"type\":\"bar|line|area|donut|pie|scatter|kpi|table|funnel|gauge|radar\",\"title\",\"subtitle\"," +
        "\"format\":\"number|currency|percent|compact\",\"unit\"}, plus categories and series [{name,data}] for bar, line, area, radar; " +
        "series [{name,data:[[x,y]]}] for scatter; slices [{name,value}] for donut, pie, funnel; kpis [{label,value,delta,hint,trend}] for kpi; " +
        "columns and rows for table; value and max for gauge. A dashboard is one kpi chart plus one chart per view. " +
        "```mermaid for flowcharts, sequences, ER, state, mindmaps, timelines, with no style or colours. ```svg for a drawing. " +
        "Put one sentence before a block and the takeaway after it. Plain text stays the default: draw when the answer is data or a flow, or when asked. Never invent numbers."
