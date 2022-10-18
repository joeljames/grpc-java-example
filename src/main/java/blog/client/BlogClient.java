package blog.client;

import com.google.protobuf.Empty;
import com.proto.blog.Blog;
import com.proto.blog.BlogId;
import com.proto.blog.BlogServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;

public class BlogClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        run(channel);

        System.out.println("Shutting down");
        channel.shutdown();
    }

    private static void run(ManagedChannel channel) {
        BlogServiceGrpc.BlogServiceBlockingStub stub = BlogServiceGrpc.newBlockingStub(channel);

        String blogId = createBlog(stub);

        //In case of an error null is returned;
        if (blogId == null) {
            return;
        }

        Blog blog = readBlog(blogId, stub);
        System.out.println("The new blog is %s:" + blog);

        updateBlog(blogId, stub);
        Blog updatedBlog = readBlog(blogId, stub);
        System.out.println("The update blog is :" + updatedBlog);

        List<Blog> blogs = getBlogs(stub);
        System.out.println("Listing the blogs in the collection");
        blogs.forEach(System.out::println);

        deleteBlog(blogId, stub);
    }

    private static void deleteBlog(String blogId, BlogServiceGrpc.BlogServiceBlockingStub stub) {
        try{
            stub.deleteBlog(BlogId.newBuilder().setId(blogId).build());
            System.out.printf("Blog delete with id %s.%n", blogId);

        } catch (StatusRuntimeException ex ) {
            System.out.printf("Failed to delete blog with id %s.%n", blogId);
            ex.printStackTrace();
        }
    }

    private static List<Blog> getBlogs(BlogServiceGrpc.BlogServiceBlockingStub stub) {
        List<Blog> blogs = new ArrayList<>();
         stub.listBlogs(Empty.getDefaultInstance()).forEachRemaining(blogs::add);
         return blogs;
    }

    private static void updateBlog(String blogId, BlogServiceGrpc.BlogServiceBlockingStub stub) {

        try {
            stub.updateBlog(Blog.newBuilder()
                    .setId(blogId)
                    .setAuthor("New Author")
                    .setContent("New Content")
                    .setTitle("New Title")
                    .build());

        } catch (StatusRuntimeException ex) {
            System.out.printf("Failed to update the blog with id %s.%n", blogId);
            ex.printStackTrace();
        }
    }

    private static Blog readBlog(String blogId, BlogServiceGrpc.BlogServiceBlockingStub stub) {
        System.out.println("Entered readBlog");

        Blog blog = null;
        try {
            blog = stub.readBlog(BlogId.newBuilder().setId(blogId).build());
        } catch (StatusRuntimeException ex) {
            System.out.printf("Failed to read the blog with id %s.%n", blogId);
            ex.printStackTrace();
        }

        return blog;
    }

    private static String createBlog(BlogServiceGrpc.BlogServiceBlockingStub stub) {
        System.out.println("Entered createBlog");

        String id = null;

        try {
            BlogId response = stub.createBlog(
                    Blog.newBuilder()
                            .setAuthor("Foo")
                            .setTitle("New Blog")
                            .setContent("This is my new blog")
                            .build());
            id = response.getId();
            System.out.println("createBlog: " + id);

            return response.getId();
        } catch (StatusRuntimeException ex) {
            System.out.println("Failed to create the blog.");
            ex.printStackTrace();
        }

        return id;
    }
}
