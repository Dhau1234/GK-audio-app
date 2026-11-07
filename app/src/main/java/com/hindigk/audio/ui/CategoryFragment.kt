package com.hindigk.audio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hindigk.audio.R
import com.hindigk.audio.data.Blog
import com.hindigk.audio.data.BloggerRepo
import com.hindigk.audio.data.Blogs
import com.hindigk.audio.databinding.FragmentCategoryBinding
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var blog: Blog
    private lateinit var adapter: PostAdapter
    private var lastFetch = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pos = arguments?.getInt(ARG_POS) ?: 0
        blog = Blogs.all[pos]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PostAdapter { post -> openPlayer(post) }
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter
        binding.swipe.setColorSchemeResources(R.color.brand)
        binding.swipe.setOnRefreshListener { reload(force = true, silent = true) }
        binding.btnRetry.setOnClickListener { reload(force = true, silent = false) }

        // 1) Cache turant dikhao (smooth start), 2) background me live network fetch
        val cached = BloggerRepo.getCached(requireContext(), blog)
        if (cached != null && cached.isNotEmpty()) {
            adapter.submitList(cached)
            showList()
        }
        reload(force = cached == null, silent = cached != null)
    }

    override fun onResume() {
        super.onResume()
        // 10 min ke baad tab wapas kholne par naye posts layaan karo (auto-update)
        if (System.currentTimeMillis() - lastFetch > 10 * 60_000) {
            reload(force = true, silent = true)
        }
    }

    private fun reload(force: Boolean, silent: Boolean) {
        if (!silent) {
            binding.progress.visibility = View.VISIBLE
            binding.errBox.visibility = View.GONE
        }
        lastFetch = System.currentTimeMillis()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val posts = BloggerRepo.fetchBlog(blog)
                if (force || adapter.currentList.size < posts.size || adapter.currentList.firstOrNull()?.title != posts.firstOrNull()?.title) {
                    adapter.submitList(posts)
                    BloggerRepo.saveCache(requireContext(), blog, posts)
                    if (posts.isEmpty()) {
                        binding.tvError.text = "इस ब्लॉग में अभी कोई पोस्ट नहीं है"
                        showError()
                    } else showList()
                } else {
                    if (binding.rvPosts.visibility == View.GONE && adapter.currentList.isNotEmpty()) showList()
                }
            } catch (e: Exception) {
                if (adapter.currentList.isEmpty()) {
                    binding.tvError.text = getString(R.string.error_network)
                    showError()
                }
            } finally {
                binding.swipe.isRefreshing = false
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showList() {
        binding.rvPosts.visibility = View.VISIBLE
        binding.errBox.visibility = View.GONE
    }

    private fun showError() {
        binding.rvPosts.visibility = View.GONE
        binding.errBox.visibility = View.VISIBLE
    }

    private fun openPlayer(post: com.hindigk.audio.data.BlogPost) {
        try {
            val i = android.content.Intent(requireContext(), PlayerActivity::class.java)
            i.putExtra(PlayerActivity.EXTRA_POST, post)
            startActivity(i)
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POS = "pos"
        fun newInstance(pos: Int) = CategoryFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POS, pos) }
        }
    }
}
