package it.faustobe.santibailor.presentation.features.note;

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
import it.faustobe.santibailor.domain.model.Nota;

/**
 * Adapter per la RecyclerView delle note
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Nota> note = new ArrayList<>();
    private final OnNotaClickListener listener;

    public NoteAdapter(OnNotaClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nota, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Nota nota = note.get(position);
        holder.bind(nota, listener);
    }

    @Override
    public int getItemCount() {
        return note.size();
    }

    /**
     * Aggiorna la lista di note
     */
    public void setNote(List<Nota> note) {
        this.note = note != null ? note : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder per una singola nota
     */
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitolo;
        private final TextView tvContenuto;
        private final TextView tvDataModifica;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitolo = itemView.findViewById(R.id.tv_nota_titolo);
            tvContenuto = itemView.findViewById(R.id.tv_nota_contenuto);
            tvDataModifica = itemView.findViewById(R.id.tv_nota_data_modifica);
        }

        public void bind(Nota nota, OnNotaClickListener listener) {
            // Imposta titolo
            tvTitolo.setText(nota.getTitolo());

            // Imposta anteprima contenuto (max 100 caratteri)
            String contenuto = nota.getContenuto();
            if (contenuto != null && !contenuto.isEmpty()) {
                if (contenuto.length() > 100) {
                    contenuto = contenuto.substring(0, 100) + "...";
                }
                tvContenuto.setText(contenuto);
                tvContenuto.setVisibility(View.VISIBLE);
            } else {
                tvContenuto.setVisibility(View.GONE);
            }

            // Formatta e imposta data modifica
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dataFormatted = sdf.format(new Date(nota.getDataModifica()));
            tvDataModifica.setText(itemView.getContext().getString(R.string.note_modified, dataFormatted));

            // Click normale
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotaClick(nota);
                }
            });

            // Long click per azioni aggiuntive
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onNotaLongClick(nota);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Interface per gestire i click sulle note
     */
    public interface OnNotaClickListener {
        void onNotaClick(Nota nota);
        void onNotaLongClick(Nota nota);
    }
}
