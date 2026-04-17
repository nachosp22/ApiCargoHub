package com.cargohub.mobile.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.FotoCarga;

import java.util.ArrayList;
import java.util.List;

public class FotoCargaAdapter extends RecyclerView.Adapter<FotoCargaAdapter.FotoViewHolder> {

    private final List<FotoCarga> fotos = new ArrayList<>();
    private OnFotoActionListener listener;
    private boolean showDelete = true;

    public interface OnFotoActionListener {
        void onFotoClick(FotoCarga foto);
        void onFotoDelete(FotoCarga foto);
    }

    public void setOnFotoActionListener(OnFotoActionListener listener) {
        this.listener = listener;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
    }

    public void setFotos(List<FotoCarga> nuevas) {
        fotos.clear();
        if (nuevas != null) {
            fotos.addAll(nuevas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_foto_carga, parent, false);
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        holder.bind(fotos.get(position));
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    class FotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnailImage;
        private final TextView tipoBadge;
        private final TextView descripcionText;
        private final ImageButton deleteButton;

        FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.fotoThumbnail);
            tipoBadge = itemView.findViewById(R.id.fotoTipoBadge);
            descripcionText = itemView.findViewById(R.id.fotoDescripcion);
            deleteButton = itemView.findViewById(R.id.fotoDeleteButton);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFotoClick(fotos.get(pos));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFotoDelete(fotos.get(pos));
                }
            });
        }

        void bind(FotoCarga foto) {
            // Decode base64 thumbnail
            try {
                String base64 = foto.getFotoBase64();
                if (base64 != null && !base64.isEmpty()) {
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    thumbnailImage.setImageBitmap(bitmap);
                } else {
                    thumbnailImage.setImageResource(R.drawable.ic_photo_placeholder);
                }
            } catch (Exception e) {
                thumbnailImage.setImageResource(R.drawable.ic_photo_placeholder);
            }

            tipoBadge.setText(foto.getTipoLabel());

            String desc = foto.getDescripcion();
            if (desc != null && !desc.isEmpty()) {
                descripcionText.setText(desc);
                descripcionText.setVisibility(View.VISIBLE);
            } else {
                descripcionText.setVisibility(View.GONE);
            }

            deleteButton.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        }
    }
}
