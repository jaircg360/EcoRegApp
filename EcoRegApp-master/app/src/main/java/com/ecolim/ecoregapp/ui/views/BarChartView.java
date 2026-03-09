package com.ecolim.ecoregapp.ui.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    public static class Barra {
        public String label;
        public float valor;
        public int color;
        public Barra(String label, float valor, int color) {
            this.label = label; this.valor = valor; this.color = color;
        }
    }

    private final List<Barra> barras = new ArrayList<>();
    private final Paint paintBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context ctx) { super(ctx); init(); }
    public BarChartView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        paintText.setTextSize(26f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintGrid.setColor(Color.parseColor("#E0E0E0"));
        paintGrid.setStrokeWidth(1f);
    }

    public void setDatos(List<Barra> datos) {
        barras.clear();
        barras.addAll(datos);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (barras.isEmpty()) return;

        int w = getWidth(), h = getHeight();
        float padL = 20f, padR = 20f, padT = 20f, padB = 50f;
        float areaW = w - padL - padR;
        float areaH = h - padT - padB;

        float maxVal = 0;
        for (Barra b : barras) if (b.valor > maxVal) maxVal = b.valor;
        if (maxVal == 0) maxVal = 1;

        // Líneas de cuadrícula
        for (int i = 0; i <= 4; i++) {
            float y = padT + areaH - (areaH * i / 4f);
            canvas.drawLine(padL, y, w - padR, y, paintGrid);
        }

        float barW = areaW / barras.size();
        float gap = barW * 0.2f;

        for (int i = 0; i < barras.size(); i++) {
            Barra b = barras.get(i);
            float barH = (b.valor / maxVal) * areaH;
            float left  = padL + i * barW + gap / 2f;
            float right = left + barW - gap;
            float top   = padT + areaH - barH;
            float bottom = padT + areaH;

            // Barra con esquinas redondeadas
            paintBarra.setColor(b.color);
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 8f, 8f, paintBarra);

            // Valor encima
            if (b.valor > 0) {
                paintText.setColor(Color.parseColor("#333333"));
                paintText.setTextSize(22f);
                String val = b.valor >= 10 ? String.format("%.0f", b.valor)
                           : String.format("%.1f", b.valor);
                canvas.drawText(val, (left + right) / 2f, top - 6f, paintText);
            }

            // Label abajo
            paintText.setColor(Color.parseColor("#666666"));
            paintText.setTextSize(24f);
            canvas.drawText(b.label, (left + right) / 2f, h - 10f, paintText);
        }
    }
}
