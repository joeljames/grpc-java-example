package blog.server;

import com.google.protobuf.Empty;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.proto.blog.Blog;
import com.proto.blog.BlogId;
import com.proto.blog.BlogServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.bson.Document;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class BlogServiceImpl extends BlogServiceGrpc.BlogServiceImplBase {
    private final MongoCollection<Document> collection;

    BlogServiceImpl(MongoClient client) {
        //Creates if does not exist or else gets the existing one.
        MongoDatabase db = client.getDatabase("blogdb");
        this.collection = db.getCollection("blog");
    }

    @Override
    public void createBlog(Blog request,
                           StreamObserver<BlogId> responseObserver) {
        Document doc = new Document("author", request.getAuthor())
                .append("title", request.getTitle())
                .append("content", request.getContent());
        InsertOneResult result;
        try {
            result = collection.insertOne(doc);

        } catch (MongoException ex) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .asRuntimeException());
            return;
        }

        if (!result.wasAcknowledged() || result.getInsertedId() == null) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Blog could not be created")
                            .asRuntimeException());
            return;
        }

        String id = result.getInsertedId().asObjectId().getValue().toString();
        responseObserver.onNext(BlogId.newBuilder().setId(id).build());
        responseObserver.onCompleted();
    }

    @Override
    public void readBlog(BlogId request,
                         StreamObserver<Blog> responseObserver) {
        String id = request.getId();

        if (id.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .augmentDescription("id is required")
                            .asRuntimeException());
            return;
        }

        Document result = collection.find(eq("_id", new ObjectId(id))).first();

        if (result == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .augmentDescription(String.format("id %s not found.", id))
                            .asRuntimeException());
            return;
        }

        responseObserver.onNext(Blog.newBuilder()
                .setAuthor(result.getString("author"))
                .setTitle(result.getString("title"))
                .setContent(result.getString("content"))
                .build());
        responseObserver.onCompleted();
    }

    public void updateBlog(Blog request,
                           StreamObserver<Empty> responseObserver) {
        String id = request.getId();
        if (id.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .augmentDescription("Blog id is required")
                            .asRuntimeException());
            return;
        }

        Document result = collection.findOneAndUpdate(eq("_id", new ObjectId(id)),
                combine(
                        set("author", request.getAuthor()),
                        set("title", request.getTitle()),
                        set("content", request.getContent())
                )
        );

        if (result == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .augmentDescription(String.format("Blog id %s not found.", id))
                            .asRuntimeException());
            return;
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void listBlogs(com.google.protobuf.Empty request, StreamObserver<Blog> responseObserver) {
        collection.find().forEach(d -> {
            Blog blog = Blog.newBuilder()
                    .setId(d.getObjectId("_id").toString())
                    .setAuthor(d.getString("author"))
                    .setTitle(d.getString("title"))
                    .setContent(d.getString("content"))
                    .build();
            responseObserver.onNext(blog);
        });

        responseObserver.onCompleted();
    }

    @Override
    public void deleteBlog(BlogId request, StreamObserver<Empty> responseObserver) {
        String blogId = request.getId();

        if (blogId.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .augmentDescription("Blog id is required")
                    .asRuntimeException());
            return;
        }

        DeleteResult result;

        try {
            result = collection.deleteOne(eq("_id", new ObjectId(blogId)));

        } catch (MongoException ex) {
            responseObserver.onError(Status.INTERNAL
                    .augmentDescription("Unable to delete blog with id " + blogId)
                    .asRuntimeException());
            return;
        }

        if (!result.wasAcknowledged() || result.getDeletedCount() == 0) {
            responseObserver.onError(Status.INTERNAL
                    .augmentDescription("Unable to delete blog with id " + blogId)
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();

    }
}
