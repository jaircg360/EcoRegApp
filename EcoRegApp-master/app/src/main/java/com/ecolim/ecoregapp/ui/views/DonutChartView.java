package com.ecolim.ecoregapp.ui.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DonutChartView extends View {

    public static class Segmento {
        public String label;
        public float valor;
        public int color;
        public Segmento(String label, float valor, int color) {
            this.label = label; this.valor = valor; this.color = color;
        }
    }

    private final List<Segmento> segmentos = new ArrayList<>();
    private final Paint paintArc  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String labelCenter = "";
    private String subLabelCenter = "";

    public DonutChartView(Context ctx) { super(ctx); init(); }
    public DonutChartView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(28f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintCenter.setColor(Color.parseColor("#1B5E20"));
        paintCenter.setTextAlign(Paint.Align.CENTER);
    }

    public void setDatos(List<Segmento> datos, String centro, String subCentro) {
        segmentos.clear();
        segmentos.addAll(datos);
        labelCenter = centro;
        subLabelCenter = subCentro;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (segmentos.isEmpty()) return;

        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radio = Math.min(cx, cy) * 0.85f;
        float radioInterno = radio * 0.55f;

        float total = 0;
        for (Segmento s : segmentos) total += s.valor;
        if (total == 0) return;

        RectF oval = new RectF(cx - radio, cy - radio, cx + radio, cy + radio);
        float anguloActual = -90f;

        for (Segmento s : segmentos) {
            float sweep = (s.valor / total) * 360f;
            paintArc.setColor(s.color);
            paintArc.setStyle(Paint.Style.FILL);
            canvas.drawArc(oval, anguloActual, sweep, true, paintArc);
            anguloActual += sweep;
        }

        // Hueco central (dona)
        paintArc.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radioInterno, paintArc);

        // Texto central
        paintCenter.setTextSize(36f);
        paintCenter.setTextAlign(Paint.Align.CENTER);
        paintCenter.setColor(Color.parseColor("#1B5E20"));
        paintCenter.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(labelCenter, cx, cy + 14f, paintCenter);
        paintCenter.setTextSize(22f);
        paintCenter.setTypeface(Typeface.DEFAULT);
        paintCenter.setColor(Color.parseColor("#555555"));
        canvas.drawText(subLabelCenter, cx, cy + 38f, paintCenter);
    }
}
