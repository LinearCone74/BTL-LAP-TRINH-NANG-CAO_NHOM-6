package service;

import model.Product;
import model.Seller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
    private List<Product> products = new ArrayList<>();

    public Product addProduct(String name, String description, double startingPrice,
                              LocalDateTime startTime, LocalDateTime endTime, Seller seller) {
        Product product = new Product(name, description, startingPrice, startTime, endTime, seller);
        products.add(product);
        return product;
    }

    public void updateProduct(int productId, String newName, String newDescription, double newStartingPrice) {
        Product product = findById(productId);
        if (product != null) {
            product.setName(newName);
            product.setDescription(newDescription);
            product.setStartingPrice(newStartingPrice);
        }
    }

    public void deleteProduct(int productId) {
        Product product = findById(productId);
        if (product != null) {
            products.remove(product);
        }
    }

    public Product findById(int productId) {
        for (Product p : products) {
            if (p.getId() == productId) {
                return p;
            }
        }
        return null;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void printAllProducts() {
        for (Product p : products) {
            p.printInfo();
            System.out.println("--------------------");
        }
    }
}
