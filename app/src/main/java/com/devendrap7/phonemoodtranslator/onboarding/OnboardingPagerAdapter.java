package com.devendrap7.phonemoodtranslator.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devendrap7.phonemoodtranslator.R;

import java.util.List;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {

    public interface ActionListener {
        void onActionButtonClicked(int pageType);
    }

    private final List<OnboardingPage> pages;
    private final ActionListener actionListener;
    private final boolean[] permissionsGranted; // tracks granted state per page

    public OnboardingPagerAdapter(List<OnboardingPage> pages, ActionListener listener) {
        this.pages = pages;
        this.actionListener = listener;
        this.permissionsGranted = new boolean[pages.size()];
    }

    public void setGranted(int pageIndex, boolean granted) {
        if (pageIndex >= 0 && pageIndex < permissionsGranted.length) {
            permissionsGranted[pageIndex] = granted;
            notifyItemChanged(pageIndex);
        }
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.emoji.setText(page.emoji);
        holder.title.setText(page.title);
        holder.desc.setText(page.description);

        if (page.actionButtonText != null) {
            holder.actionBtn.setVisibility(View.VISIBLE);
            holder.actionBtn.setText(page.actionButtonText);
            holder.actionBtn.setOnClickListener(v -> actionListener.onActionButtonClicked(page.pageType));
        } else {
            holder.actionBtn.setVisibility(View.GONE);
        }

        // Show granted state
        if (permissionsGranted[position]) {
            holder.tvGranted.setVisibility(View.VISIBLE);
            holder.actionBtn.setVisibility(View.GONE);
        } else {
            holder.tvGranted.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        TextView emoji, title, desc, tvGranted;
        Button actionBtn;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.tvPageEmoji);
            title = itemView.findViewById(R.id.tvPageTitle);
            desc = itemView.findViewById(R.id.tvPageDesc);
            actionBtn = itemView.findViewById(R.id.btnPageAction);
            tvGranted = itemView.findViewById(R.id.tvGranted);
        }
    }
}
