package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(private val recipes: List<RecipeItem>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRecipeImage: ImageView = itemView.findViewById(R.id.iv_recipe_image)
        val tvRecipeName: TextView = itemView.findViewById(R.id.tv_recipe_name)
        val tvRecipeDesc: TextView = itemView.findViewById(R.id.tv_recipe_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.ivRecipeImage.setImageResource(recipe.imageResId)
        holder.tvRecipeName.text = recipe.name
        holder.tvRecipeDesc.text = recipe.description
    }

    override fun getItemCount(): Int = recipes.size
}