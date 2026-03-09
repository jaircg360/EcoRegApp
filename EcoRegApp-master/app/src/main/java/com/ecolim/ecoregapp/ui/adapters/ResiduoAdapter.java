package com.ecolim.ecoregapp.ui.adapters;

import android.content.Context;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;

public class ResiduoAdapter extends ListAdapter<Residuo, ResiduoAdapter.ViewHolder> {

    public interface OnItemClick { void onClick(Residuo r); }
    private final OnItemClick listener;

    public ResiduoAdapter(OnItemClick listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_residuo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.bind(getItem(pos), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvPeso, tvFecha, tvZona, tvEstado, tvEmoji, tvOperario;

        ViewHolder(View v) {
            super(v);
            tvTipo     = v.findViewById(R.id.tv_tipo);
            tvPeso     = v.findViewById(R.id.tv_peso);
            tvFecha    = v.findViewById(R.id.tv_fecha);
            tvZona     = v.findViewById(R.id.tv_zona);
            tvEstado   = v.findViewById(R.id.tv_estado);
            tvEmoji    = v.findViewById(R.id.tv_emoji);
            tvOperario = v.findViewById(R.id.tv_operario);
        }

        void bind(Residuo r, OnItemClick listener) {
            // Emoji + tipo
            String emoji;
            switch (r.tipo != null ? r.tipo.toLowerCase() : "") {
                case "plastico":  emoji = "♻️"; break;
                case "organico":  emoji = "🌿"; break;
                case "papel":     emoji = "📄"; break;
                case "metal":     emoji = "🔩"; break;
                case "vidrio":    emoji = "🫙"; break;
                case "peligroso": emoji = "⚠️"; break;
                default:          emoji = "🗑️"; break;
            }
            tvEmoji.setText(emoji);
            tvTipo.setText(capitalizar(r.tipo));
            tvPeso.setText(String.format("%.1f kg", r.pesoKg));

            // Zona o ubicación
            String zona = (r.zona != null && !r.zona.isEmpty()) ? r.zona
                        : (r.ubicacion != null && !r.ubicacion.isEmpty()) ? r.ubicacion
                        : "—";
            tvZona.setText(zona);

            // Fecha (solo fecha, sin hora)
            tvFecha.setText(r.fecha != null
                ? r.fecha.substring(0, Math.min(10, r.fecha.length())) : "—");

            // Operario
            String op = (r.operarioNombre != null && !r.operarioNombre.isEmpty())
                ? "Operario " + r.operarioId
                : (r.operarioId != null ? r.operarioId : "—");
            tvOperario.setText(op);

            // Badge sync
            if (r.sincronizado) {
                tvEstado.setText("✓ Sync");
                tvEstado.setBackgroundResource(R.drawable.bg_badge_green);
            } else {
                tvEstado.setText("⏳ Pend.");
                tvEstado.setBackgroundResource(R.drawable.bg_badge_yellow);
            }

            itemView.setOnClickListener(v -> listener.onClick(r));
        }

        private String capitalizar(String s) {
            if (s == null || s.isEmpty()) return "—";
            return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        }
    }

    private static final DiffUtil.ItemCallback<Residuo> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Residuo>() {
            @Override public boolean areItemsTheSame(@NonNull Residuo a, @NonNull Residuo b) {
                return a.id == b.id;
            }
            @Override public boolean areContentsTheSame(@NonNull Residuo a, @NonNull Residuo b) {
                return a.sincronizado == b.sincronizado
                    && a.pesoKg == b.pesoKg
                    && (a.tipo != null && a.tipo.equals(b.tipo));
            }
        };
}
