package failchat.youtube

data class MetadataResponse(
        val actions: List<Action>
) {

    data class Action(
            val updateViewershipAction: UpdateViewershipAction?
    )

    data class UpdateViewershipAction(
            val viewCount: ViewCount
    )

    data class ViewCount(
            val videoViewCountRenderer: VideoViewCountRenderer
    )

    data class VideoViewCountRenderer(
            val viewCount: ViewCountRendered
    )

    data class ViewCountRendered(
            val simpleText: String
    )

}
