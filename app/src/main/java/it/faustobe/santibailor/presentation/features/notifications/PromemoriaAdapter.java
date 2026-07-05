package it.faustobe.santibailor.presentation.features.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.faustobe.santibailor.R;
import it.faustobe.santibailor.domain.model.Impegno;

/**
 * Adapter per la lista dei promemoria programmati
 */
public class PromemoriaAdapter extends RecyclerView.Adapter<PromemoriaAdapter.PromemoriaViewHolder> {

    private List<Impegno> impegni = new ArrayList<>();
    private final OnPromemoriaClickListener listener;

    public PromemoriaAdapter(OnPromemoriaClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromemoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promemoria, parent, false);
        return new PromemoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromemoriaViewHolder holder, int position) {
        holder.bind(impegni.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return impegni.size();
    }

    public void setImpegni(List<Impegno> impegni) {
        this.impegni = impegni != null ? impegni : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class PromemoriaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitolo;
        private final TextView tvDataOra;
        private final TextView tvPromemoria;

        public PromemoriaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitolo = itemView.findViewById(R.id.tv_promemoria_titolo);
            tvDataOra = itemView.findViewById(R.id.tv_promemoria_data_ora);
            tvPromemoria = itemView.findViewById(R.id.tv_promemoria_info);
        }

        public void bind(Impegno impegno, OnPromemoriaClickListener listener) {
            tvTitolo.setText(impegno.getTitolo());

            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE dd MMMM yyyy, HH:mm", Locale.getDefault());
            tvDataOra.setText(dateTimeFormat.format(new Date(impegno.getDataOra())));

            long reminderAt = impegno.getDataOra() - impegno.getReminderMinutesBefore() * 60_000L;
            SimpleDateFormat reminderFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            tvPromemoria.setText(itemView.getContext().getString(
                    R.string.promemoria_info_format,
                    reminderFormat.format(new Date(reminderAt)),
                    impegno.getReminderMinutesBefore()
            ));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromemoriaClick(impegno);
                }
            });
        }
    }

    public interface OnPromemoriaClickListener {
        void onPromemoriaClick(Impegno impegno);
    }
}
